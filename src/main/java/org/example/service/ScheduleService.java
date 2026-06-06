package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.Personnel;
import org.example.entity.ScheduleEntry;
import org.example.repository.PersonnelRepository;
import org.example.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepo;
    private final PersonnelRepository personnelRepo;

    public ScheduleService(ScheduleRepository scheduleRepo,
                           PersonnelRepository personnelRepo) {
        this.scheduleRepo = scheduleRepo;
        this.personnelRepo = personnelRepo;
    }

    // В кінець класу ScheduleService (перед допоміжними методами)

    public List<Integer> getYears() {
        return scheduleRepo.findDistinctYears();
    }

    public List<String> getMonths() {
        List<Integer> monthNumbers = scheduleRepo.findDistinctMonthNumbers();
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        return monthNumbers.stream()
                .map(num -> monthNames[num - 1])
                .collect(Collectors.toList());
    }

    // ========== ЕКСПОРТ В XLSX ==========

    /**
     * Експорт за рік і місяць (якщо month != null)
     * Якщо month == null, експортує всі місяці вказаного року (по одному аркушу на місяць)
     */
    public byte[] exportToXlsx(Integer year, Integer month) throws Exception {
        if (month != null) {
            // Експорт за конкретний місяць
            return exportMonthToXlsx(year, month);
        } else if (year != null) {
            // Експорт за весь рік (окремі аркуші для кожного місяця, де є дані)
            return exportYearToXlsx(year);
        } else {
            throw new IllegalArgumentException("Необхідно вказати рік або рік+місяць");
        }
    }

    // ========== ЕКСПОРТ В XLSX (оновлений) ==========

    private byte[] exportMonthToXlsx(int year, int month) throws Exception {
        List<Map<String, Object>> rows = getMonthData(year, month);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Немає даних за вибраний місяць");
        }

        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet(String.format("%d_%02d", year, month));

            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataCenterStyle = createDataStyle(wb); // для №
            XSSFCellStyle dataLeftStyle = createLeftAlignedDataStyle(wb); // для ПІБ, Звання

            // Заголовки
            String[] headers = new String[3 + daysInMonth];
            headers[0] = "#";
            headers[1] = "ПІБ";
            headers[2] = "Звання";
            for (int d = 1; d <= daysInMonth; d++) {
                headers[2 + d] = String.valueOf(d);
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 24 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            for (int d = 1; d <= daysInMonth; d++) {
                sheet.setColumnWidth(2 + d, 10 * 256);
            }

            XSSFRow headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Map<String, Object> rowMap : rows) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(18);

                // № – по центру
                setCell(dataRow, 0, rowNum - 1, dataCenterStyle);
                // ПІБ – ліворуч
                setCell(dataRow, 1, rowMap.get("shortName"), dataLeftStyle);
                // Звання – ліворуч
                setCell(dataRow, 2, rowMap.get("rank"), dataLeftStyle);

                @SuppressWarnings("unchecked")
                Map<Integer, String> daysMap = (Map<Integer, String>) rowMap.get("days");
                for (int d = 1; d <= daysInMonth; d++) {
                    String statusCode = daysMap.get(d);
                    String display = mapStatusToLabel(statusCode);
                    XSSFCellStyle statusStyle = getStatusStyle(statusCode, wb);
                    setCell(dataRow, 2 + d, display, statusStyle);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportYearToXlsx(int year) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int month = 1; month <= 12; month++) {
                List<Map<String, Object>> rows = getMonthData(year, month);
                if (rows.isEmpty()) continue;

                int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
                XSSFSheet sheet = wb.createSheet(String.format("%d_%02d", year, month));

                XSSFCellStyle headerStyle = createHeaderStyle(wb);
                XSSFCellStyle dataCenterStyle = createDataStyle(wb);
                XSSFCellStyle dataLeftStyle = createLeftAlignedDataStyle(wb);

                String[] headers = new String[3 + daysInMonth];
                headers[0] = "#";
                headers[1] = "ПІБ";
                headers[2] = "Звання";
                for (int d = 1; d <= daysInMonth; d++) {
                    headers[2 + d] = String.valueOf(d);
                }

                sheet.setColumnWidth(0, 8 * 256);
                sheet.setColumnWidth(1, 24 * 256);
                sheet.setColumnWidth(2, 18 * 256);
                for (int d = 1; d <= daysInMonth; d++) {
                    sheet.setColumnWidth(2 + d, 10 * 256);
                }

                XSSFRow headerRow = sheet.createRow(0);
                headerRow.setHeightInPoints(25);
                for (int i = 0; i < headers.length; i++) {
                    XSSFCell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowNum = 1;
                for (Map<String, Object> rowMap : rows) {
                    XSSFRow dataRow = sheet.createRow(rowNum++);
                    dataRow.setHeightInPoints(18);

                    setCell(dataRow, 0, rowNum - 1, dataCenterStyle);
                    setCell(dataRow, 1, rowMap.get("shortName"), dataLeftStyle);
                    setCell(dataRow, 2, rowMap.get("rank"), dataLeftStyle);

                    @SuppressWarnings("unchecked")
                    Map<Integer, String> daysMap = (Map<Integer, String>) rowMap.get("days");
                    for (int d = 1; d <= daysInMonth; d++) {
                        String statusCode = daysMap.get(d);
                        String display = mapStatusToLabel(statusCode);
                        XSSFCellStyle statusStyle = getStatusStyle(statusCode, wb);
                        setCell(dataRow, 2 + d, display, statusStyle);
                    }
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

// ========== НОВІ ДОПОМІЖНІ МЕТОДИ ==========

    /**
     * Стиль для комірок з вирівнюванням по лівому краю (для ПІБ, Звання)
     */
    private XSSFCellStyle createLeftAlignedDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    /**
     * Повертає стиль для комірки статусу з відповідним кольором фону
     * (кольори відповідають CSS-класам на веб-сторінці)
     */
    private XSSFCellStyle getStatusStyle(String statusCode, XSSFWorkbook wb) {
        // Мапа кольорів для кожного статусу (RGB)
        Map<String, byte[]> colorMap = new HashMap<>();
        colorMap.put("БЧ",     new byte[]{(byte)255, (byte)205, (byte)210}); // #ffcdd2
        colorMap.put("БЧ БАТ", new byte[]{(byte)239, (byte)154, (byte)154}); // #ef9a9a
        colorMap.put("БЧ РКП", new byte[]{(byte)255, (byte)171, (byte)145}); // #ffab91
        colorMap.put("Р-НЯ",   new byte[]{(byte)200, (byte)230, (byte)201}); // #c8e6c9
        colorMap.put("В-НЯ",   new byte[]{(byte)187, (byte)222, (byte)251}); // #bbdefb
        colorMap.put("В-КА",   new byte[]{(byte)179, (byte)229, (byte)252}); // #b3e5fc
        colorMap.put("ХВ",     new byte[]{(byte)248, (byte)187, (byte)208}); // #f8bbd0
        colorMap.put("СЗЧ",    new byte[]{(byte)255, (byte)224, (byte)178}); // #ffe0b2
        colorMap.put("ПЛЮ",    new byte[]{(byte)225, (byte)190, (byte)231}); // #e1bee7
        colorMap.put("ППД",    new byte[]{(byte)220, (byte)237, (byte)200}); // #dcedc8
        colorMap.put("НАВ",    new byte[]{(byte)255, (byte)249, (byte)196}); // #fff9c4
        colorMap.put("ПЕРЕВ",  new byte[]{(byte)207, (byte)216, (byte)220}); // #cfd8dc

        if (statusCode == null || statusCode.isBlank()) {
            return createDataStyle(wb); // порожня комірка – стандартний стиль
        }

        byte[] rgb = colorMap.get(statusCode);
        if (rgb == null) {
            return createDataStyle(wb);
        }

        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    // ========== ОСНОВНІ МЕТОДИ ==========

    public List<Map<String, Object>> getMonthData(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        List<Personnel> allPersonnel = personnelRepo.findByActiveTrueOrderByLastNameAsc();
        List<ScheduleEntry> entries = scheduleRepo.findByMonth(from, to);

        Map<Long, Map<Integer, String>> index = new HashMap<>();
        for (ScheduleEntry e : entries) {
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
        Optional<ScheduleEntry> existing = scheduleRepo.findByPersonnelIdAndDate(personnelId, date);

        if (status == null || status.isBlank()) {
            existing.ifPresent(e -> {
                scheduleRepo.delete(e);
                log.info("Видалено запис: боєць={}, дата={}", personnelId, date);
            });
            return;
        }

        if (existing.isPresent()) {
            existing.get().setStatus(status);
            scheduleRepo.save(existing.get());
        } else {
            Personnel p = personnelRepo.findById(personnelId)
                    .orElseThrow(() -> new IllegalArgumentException("Боєць не знайдений: " + personnelId));
            scheduleRepo.save(new ScheduleEntry(p, date, status));
        }
        log.info("Графік: боєць={}, дата={}, статус={}", personnelId, date, status);
    }

    public Map<Long, Map<String, Long>> getMonthlyCounts(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<ScheduleEntry> entries = scheduleRepo.findByMonth(ym.atDay(1), ym.atEndOfMonth());

        Map<Long, Map<String, Long>> result = new HashMap<>();
        for (ScheduleEntry e : entries) {
            Long pid = e.getPersonnel().getId();
            result.computeIfAbsent(pid, k -> new HashMap<>())
                    .merge(e.getStatus(), 1L, Long::sum);
        }
        return result;
    }

    // ========== ДОПОМІЖНІ МЕТОДИ ДЛЯ XLSX ==========

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{26, 35, 126}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        // Виправлення для білого кольору:
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    private void setBorders(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String mapStatusToLabel(String code) {
        if (code == null) return "";
        switch (code) {
            case "БЧ": return "БЧ";
            case "БЧ БАТ": return "БЧ Бат";
            case "БЧ РКП": return "БЧ РКП";
            case "Р-НЯ": return "Р-ня";
            case "В-НЯ": return "В-ня";
            case "В-КА": return "В-ка";
            case "ХВ": return "Х-й";
            case "СЗЧ": return "СЗЧ";
            case "ПЛЮ": return "Плюс";
            case "ППД": return "ППД";
            case "НАВ": return "Навчання";
            case "ПЕРЕВ": return "Перевівся";
            default: return code;
        }
    }
}