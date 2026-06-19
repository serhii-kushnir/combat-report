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

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepo;
    private final PersonnelRepository personnelRepo;

    // Мапа скорочень днів тижня українською
    private static final Map<java.time.DayOfWeek, String> DAY_ABBREVIATIONS = new HashMap<>();
    static {
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.MONDAY, "Пн");
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.TUESDAY, "Вт");
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.WEDNESDAY, "Ср");
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.THURSDAY, "Чт");
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.FRIDAY, "Пт");
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.SATURDAY, "Сб");
        DAY_ABBREVIATIONS.put(java.time.DayOfWeek.SUNDAY, "Нд");
    }

    public ScheduleService(ScheduleRepository scheduleRepo,
                           PersonnelRepository personnelRepo) {
        this.scheduleRepo = scheduleRepo;
        this.personnelRepo = personnelRepo;
    }

    // ===== ОСНОВНІ МЕТОДИ =====

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

    // ===== ЕКСПОРТ В XLSX =====

    public byte[] exportToXlsx(Integer year, Integer month) throws Exception {
        if (month != null) {
            return exportMonthToXlsx(year, month);
        } else if (year != null) {
            return exportYearToXlsx(year);
        } else {
            throw new IllegalArgumentException("Необхідно вказати рік або рік+місяць");
        }
    }

    private byte[] exportMonthToXlsx(int year, int month) throws Exception {
        List<Map<String, Object>> rows = getMonthData(year, month);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Немає даних за вибраний місяць");
        }

        // СОРТУВАННЯ за personnelNumber (від 1 до більшого)
        rows.sort(Comparator.comparingInt(r -> {
            Integer num = (Integer) r.get("personnelNumber");
            return num != null ? num : Integer.MAX_VALUE;
        }));

        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet(String.format("%d_%02d", year, month));

            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dayNameStyle = createDayNameStyle(wb);
            XSSFCellStyle dataCenterStyle = createDataStyle(wb);
            XSSFCellStyle dataLeftStyle = createLeftAlignedDataStyle(wb);

            // === ПОРЯДОК: №, Звання, ПІБ, дні ===
            String[] headers = new String[3 + daysInMonth];
            headers[0] = "№";
            headers[1] = "Звання";
            headers[2] = "ПІБ";
            for (int d = 1; d <= daysInMonth; d++) {
                headers[2 + d] = String.valueOf(d);
            }

            // Ширини колонок
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 24 * 256);
            for (int d = 1; d <= daysInMonth; d++) {
                sheet.setColumnWidth(2 + d, 10 * 256);
            }

            // РЯДОК 0: числа місяця (заголовок)
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // РЯДОК 1: назви днів тижня (під числами)
            XSSFRow dayNameRow = sheet.createRow(1);
            dayNameRow.setHeightInPoints(16);
            // Перші три колонки залишаємо пустими або можна поставити прочерк
            for (int i = 0; i < 3; i++) {
                XSSFCell cell = dayNameRow.createCell(i);
                cell.setCellValue("");
                cell.setCellStyle(dayNameStyle);
            }
            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = LocalDate.of(year, month, d);
                java.time.DayOfWeek dow = date.getDayOfWeek();
                String abbr = DAY_ABBREVIATIONS.getOrDefault(dow, "");
                XSSFCell cell = dayNameRow.createCell(2 + d);
                cell.setCellValue(abbr);
                cell.setCellStyle(dayNameStyle);
            }

            // Дані починаються з рядка 2
            int rowNum = 2;
            for (Map<String, Object> rowMap : rows) {
                XSSFRow dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(18);

                Integer personnelNumber = (Integer) rowMap.get("personnelNumber");
                Object num = personnelNumber != null ? personnelNumber : rowNum - 2;
                setCell(dataRow, 0, num, dataCenterStyle);
                setCell(dataRow, 1, rowMap.get("rank"), dataLeftStyle);
                setCell(dataRow, 2, rowMap.get("shortName"), dataLeftStyle);

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

                // СОРТУВАННЯ за personnelNumber
                rows.sort(Comparator.comparingInt(r -> {
                    Integer num = (Integer) r.get("personnelNumber");
                    return num != null ? num : Integer.MAX_VALUE;
                }));

                int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
                XSSFSheet sheet = wb.createSheet(String.format("%d_%02d", year, month));

                XSSFCellStyle headerStyle = createHeaderStyle(wb);
                XSSFCellStyle dayNameStyle = createDayNameStyle(wb);
                XSSFCellStyle dataCenterStyle = createDataStyle(wb);
                XSSFCellStyle dataLeftStyle = createLeftAlignedDataStyle(wb);

                String[] headers = new String[3 + daysInMonth];
                headers[0] = "№";
                headers[1] = "Звання";
                headers[2] = "ПІБ";
                for (int d = 1; d <= daysInMonth; d++) {
                    headers[2 + d] = String.valueOf(d);
                }

                sheet.setColumnWidth(0, 8 * 256);
                sheet.setColumnWidth(1, 18 * 256);
                sheet.setColumnWidth(2, 24 * 256);
                for (int d = 1; d <= daysInMonth; d++) {
                    sheet.setColumnWidth(2 + d, 10 * 256);
                }

                // Рядок 0: числа
                XSSFRow headerRow = sheet.createRow(0);
                headerRow.setHeightInPoints(20);
                for (int i = 0; i < headers.length; i++) {
                    XSSFCell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Рядок 1: дні
                XSSFRow dayNameRow = sheet.createRow(1);
                dayNameRow.setHeightInPoints(16);
                for (int i = 0; i < 3; i++) {
                    XSSFCell cell = dayNameRow.createCell(i);
                    cell.setCellValue("");
                    cell.setCellStyle(dayNameStyle);
                }
                for (int d = 1; d <= daysInMonth; d++) {
                    LocalDate date = LocalDate.of(year, month, d);
                    java.time.DayOfWeek dow = date.getDayOfWeek();
                    String abbr = DAY_ABBREVIATIONS.getOrDefault(dow, "");
                    XSSFCell cell = dayNameRow.createCell(2 + d);
                    cell.setCellValue(abbr);
                    cell.setCellStyle(dayNameStyle);
                }

                int rowNum = 2;
                for (Map<String, Object> rowMap : rows) {
                    XSSFRow dataRow = sheet.createRow(rowNum++);
                    dataRow.setHeightInPoints(18);

                    Integer personnelNumber = (Integer) rowMap.get("personnelNumber");
                    Object num = personnelNumber != null ? personnelNumber : rowNum - 2;
                    setCell(dataRow, 0, num, dataCenterStyle);
                    setCell(dataRow, 1, rowMap.get("rank"), dataLeftStyle);
                    setCell(dataRow, 2, rowMap.get("shortName"), dataLeftStyle);

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

    // ===== ДОПОМІЖНІ МЕТОДИ =====

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

    private XSSFCellStyle createDayNameStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}));
        font.setBold(false);
        font.setFontHeightInPoints((short) 9);
        font.setFontName("Arial");
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{30, 40, 80}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
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

    private XSSFCellStyle createLeftAlignedDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
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

    private XSSFCellStyle getStatusStyle(String statusCode, XSSFWorkbook wb) {
        if (statusCode == null || statusCode.isBlank()) {
            return createDataStyle(wb);
        }
        Map<String, byte[]> colorMap = new HashMap<>();
        colorMap.put("БЧ",     new byte[]{(byte)255, (byte)205, (byte)210});
        colorMap.put("БЧ БАТ", new byte[]{(byte)239, (byte)154, (byte)154});
        colorMap.put("БЧ РКП", new byte[]{(byte)255, (byte)171, (byte)145});
        colorMap.put("Р-НЯ",   new byte[]{(byte)200, (byte)230, (byte)201});
        colorMap.put("В-НЯ",   new byte[]{(byte)187, (byte)222, (byte)251});
        colorMap.put("В-КА",   new byte[]{(byte)179, (byte)229, (byte)252});
        colorMap.put("ХВ",     new byte[]{(byte)248, (byte)187, (byte)208});
        colorMap.put("СЗЧ",    new byte[]{(byte)255, (byte)224, (byte)178});
        colorMap.put("ПЛЮ",    new byte[]{(byte)225, (byte)190, (byte)231});
        colorMap.put("ППД",    new byte[]{(byte)220, (byte)237, (byte)200});
        colorMap.put("НАВ",    new byte[]{(byte)255, (byte)249, (byte)196});
        colorMap.put("ПЕРЕВ",  new byte[]{(byte)207, (byte)216, (byte)220});

        byte[] rgb = colorMap.get(statusCode);
        if (rgb == null) return createDataStyle(wb);

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
}