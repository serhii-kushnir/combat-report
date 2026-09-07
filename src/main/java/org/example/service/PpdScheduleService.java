package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.Personnel;
import org.example.entity.PpdScheduleEntry;
import org.example.repository.PersonnelRepository;
import org.example.repository.PpdScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class PpdScheduleService {

    private static final Logger log = LoggerFactory.getLogger(PpdScheduleService.class);

    private final PpdScheduleRepository scheduleRepo;
    private final PersonnelRepository personnelRepo;

    public PpdScheduleService(PpdScheduleRepository scheduleRepo,
                              PersonnelRepository personnelRepo) {
        this.scheduleRepo = scheduleRepo;
        this.personnelRepo = personnelRepo;
    }

    public List<Map<String, Object>> getMonthData(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        List<Personnel> allPersonnel = personnelRepo.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        List<PpdScheduleEntry> entries = scheduleRepo.findByMonth(from, to);

        Map<Long, Map<Integer, String>> index = new HashMap<>();
        for (PpdScheduleEntry e : entries) {
            Long pid = e.getPersonnel().getId();
            index.computeIfAbsent(pid, k -> new HashMap<>())
                    .put(e.getDate().getDayOfMonth(), e.getStatus());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Personnel p : allPersonnel) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("shortName", p.getShortName());
            row.put("rank", p.getRank() != null ? p.getRank() : "");
            row.put("personnelNumber", p.getPersonnelNumber());

            Map<Integer, String> days = new LinkedHashMap<>();
            Map<Integer, String> personDays = index.getOrDefault(p.getId(), Collections.emptyMap());
            for (int d = 1; d <= daysInMonth; d++) {
                days.put(d, personDays.getOrDefault(d, ""));
            }
            row.put("days", days);
            rows.add(row);
        }
        return rows;
    }

    @Transactional
    public void setStatus(Long personnelId, LocalDate date, String status) {
        Optional<PpdScheduleEntry> existing = scheduleRepo.findByPersonnelIdAndDate(personnelId, date);

        if (status == null || status.isBlank()) {
            existing.ifPresent(e -> {
                scheduleRepo.delete(e);
                log.info("Видалено запис ППД: боєць={}, дата={}", personnelId, date);
            });
            return;
        }

        if (existing.isPresent()) {
            existing.get().setStatus(status);
            scheduleRepo.save(existing.get());
        } else {
            Personnel p = personnelRepo.findById(personnelId)
                    .orElseThrow(() -> new IllegalArgumentException("Боєць не знайдений: " + personnelId));
            scheduleRepo.save(new PpdScheduleEntry(p, date, status));
        }
        log.info("Графік ППД: боєць={}, дата={}, статус={}", personnelId, date, status);
    }

    public List<Integer> getYears() {
        return scheduleRepo.findDistinctYears();
    }

    public List<String> getMonths() {
        List<Integer> monthNumbers = scheduleRepo.findDistinctMonthNumbers();
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        return monthNumbers.stream()
                .map(num -> monthNames[num - 1])
                .toList();
    }

    // ===== ЕКСПОРТ ОДНОГО МІСЯЦЯ =====
    public byte[] exportToXlsx(int year, int month) throws Exception {
        List<Map<String, Object>> rows = getMonthData(year, month);
        rows.sort(Comparator.comparingInt(r -> {
            Integer num = (Integer) r.get("personnelNumber");
            return num != null ? num : Integer.MAX_VALUE;
        }));

        List<Map<String, Object>> stats = buildStatsFromRows(rows);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String monthName = getMonthName(month);
            XSSFSheet sheet1 = wb.createSheet(monthName + " Графік");
            buildScheduleSheet(sheet1, rows, year, month);

            XSSFSheet sheet2 = wb.createSheet(monthName + " Статистика");
            buildStatsSheet(sheet2, stats);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ЕКСПОРТ ВСЬОГО РОКУ (12 АРКУШІВ) =====
    public byte[] exportFullYearToXlsx(int year) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int month = 1; month <= 12; month++) {
                List<Map<String, Object>> rows = getMonthData(year, month);
                if (rows.isEmpty()) continue;

                rows.sort(Comparator.comparingInt(r -> {
                    Integer num = (Integer) r.get("personnelNumber");
                    return num != null ? num : Integer.MAX_VALUE;
                }));

                List<Map<String, Object>> stats = buildStatsFromRows(rows);
                String monthName = getMonthName(month);

                XSSFSheet sheet1 = wb.createSheet(monthName + " Графік");
                buildScheduleSheet(sheet1, rows, year, month);

                XSSFSheet sheet2 = wb.createSheet(monthName + " Статистика");
                buildStatsSheet(sheet2, stats);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ДОПОМІЖНІ МЕТОДИ =====
    private List<Map<String, Object>> buildStatsFromRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> statRow = new LinkedHashMap<>(row);
            Map<Integer, String> days = (Map<Integer, String>) row.get("days");
            int total = 0;
            for (String status : days.values()) {
                if ("ППД".equals(status)) total++;
            }
            statRow.put("total", total);
            stats.add(statRow);
        }
        return stats;
    }

    private String getMonthName(int month) {
        String[] names = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        return names[month - 1];
    }

    // ===== ПОБУДОВА АРКУШІВ =====
    private void buildScheduleSheet(XSSFSheet sheet, List<Map<String, Object>> rows, int year, int month) {
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle centerStyle = createCenterStyle(sheet.getWorkbook());
        CellStyle leftStyle = createLeftStyle(sheet.getWorkbook());
        CellStyle ppdStyle = createPpdStyle(sheet.getWorkbook());

        // Заголовки
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        int col = 0;
        createCell(headerRow, col++, "№", headerStyle);
        createCell(headerRow, col++, "Звання", headerStyle);
        createCell(headerRow, col++, "ПІБ", headerStyle);
        for (int d = 1; d <= daysInMonth; d++) {
            createCell(headerRow, col++, String.valueOf(d), headerStyle);
        }

        // Рядок з днями тижня
        Row dayNameRow = sheet.createRow(1);
        dayNameRow.setHeightInPoints(16);
        String[] daysOfWeek = {"Нд", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        for (int i = 0; i < 3; i++) createCell(dayNameRow, i, "", headerStyle);
        for (int d = 1; d <= daysInMonth; d++) {
            int dow = YearMonth.of(year, month).atDay(d).getDayOfWeek().getValue() % 7;
            createCell(dayNameRow, 2 + d, daysOfWeek[dow], headerStyle);
        }

        // Дані
        int rowNum = 2;
        for (Map<String, Object> rowMap : rows) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(20);
            col = 0;
            createCell(row, col++, rowMap.get("personnelNumber"), centerStyle);
            createCell(row, col++, rowMap.get("rank"), leftStyle);
            createCell(row, col++, rowMap.get("shortName"), leftStyle);

            Map<Integer, String> days = (Map<Integer, String>) rowMap.get("days");
            for (int d = 1; d <= daysInMonth; d++) {
                String status = days.getOrDefault(d, "");
                CellStyle style = "ППД".equals(status) ? ppdStyle : centerStyle;
                createCell(row, col++, status, style);
            }
        }

        // Ширини
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        for (int d = 1; d <= daysInMonth; d++) {
            sheet.setColumnWidth(2 + d, 8 * 256);
        }
    }

    private void buildStatsSheet(XSSFSheet sheet, List<Map<String, Object>> stats) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle centerStyle = createCenterStyle(sheet.getWorkbook());
        CellStyle leftStyle = createLeftStyle(sheet.getWorkbook());

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        createCell(headerRow, 0, "№", headerStyle);
        createCell(headerRow, 1, "Звання", headerStyle);
        createCell(headerRow, 2, "ПІБ", headerStyle);
        createCell(headerRow, 3, "Днів ППД", headerStyle);

        int rowNum = 1;
        for (Map<String, Object> statRow : stats) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(20);
            createCell(row, 0, statRow.get("personnelNumber"), centerStyle);
            createCell(row, 1, statRow.get("rank"), leftStyle);
            createCell(row, 2, statRow.get("shortName"), leftStyle);
            createCell(row, 3, statRow.get("total"), centerStyle);
        }

        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        sheet.setColumnWidth(3, 12 * 256);
    }

    // ===== СТИЛІ =====
    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createLeftStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createPpdStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();

        // --- ВСТАНОВЛЮЄМО БІЛИЙ ЖИРНИЙ ШРИФТ ---
        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        s.setFont(font);
        // ----------------------------------------

        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        s.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private void setBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}