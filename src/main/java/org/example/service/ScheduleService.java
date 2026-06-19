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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepo;
    private final PersonnelRepository personnelRepo;

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

    private static final Map<String, String> CATEGORY_MAPPING = new HashMap<>();
    static {
        CATEGORY_MAPPING.put("БЧ", "БЧ");
        CATEGORY_MAPPING.put("БЧ БАТ", "Бат БЧ");
        CATEGORY_MAPPING.put("БЧ РКП", "БЧ РКП");
        CATEGORY_MAPPING.put("Р-НЯ", "Р-ня");
        CATEGORY_MAPPING.put("В-НЯ", "В-ня");
        CATEGORY_MAPPING.put("В-КА", "В-ка");
        CATEGORY_MAPPING.put("ХВ", "Х-й");
        CATEGORY_MAPPING.put("СЗЧ", "СЗЧ");
        CATEGORY_MAPPING.put("ПЛЮ", "Плюс");
        CATEGORY_MAPPING.put("ППД", "ППД");
        CATEGORY_MAPPING.put("НАВ", "Навчання");
        CATEGORY_MAPPING.put("ПЕРЕВ", "Перевівся");
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

        // Отримуємо тільки осіб зі статусом "В особовому складі"
        List<Personnel> allPersonnel = personnelRepo.findByPersonnelStatusAndActiveTrueOrderByLastNameAsc("В особовому складі");

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

    // ===== ЕКСПОРТ – ОДИН ФАЙЛ, ДВА АРКУШІ ДЛЯ КОЖНОГО МІСЯЦЯ =====

    /**
     * Експорт за місяць або за рік.
     * Якщо month == null, експортує всі місяці року (кожен місяць – окрема пара аркушів).
     */
    public byte[] exportCombinedXlsx(Integer year, Integer month) throws Exception {
        if (month != null) {
            return exportSingleMonth(year, month);
        } else {
            return exportWholeYear(year);
        }
    }

    // Експорт одного місяця (два аркуші: Графік і Статистика)
    private byte[] exportSingleMonth(int year, int month) throws Exception {
        List<Map<String, Object>> rows = getMonthData(year, month);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Немає даних за вибраний місяць");
        }

        rows.sort(Comparator.comparingInt(r -> {
            Integer num = (Integer) r.get("personnelNumber");
            return num != null ? num : Integer.MAX_VALUE;
        }));

        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Аркуш 1: Графік
            XSSFSheet sheet1 = wb.createSheet("Графік");
            buildScheduleSheet(sheet1, rows, year, month, daysInMonth);

            // Аркуш 2: Статистика
            XSSFSheet sheet2 = wb.createSheet("Статистика");
            buildStatsSheet(sheet2, rows);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // Експорт за весь рік (кожен місяць – окрема пара аркушів)
    private byte[] exportWholeYear(int year) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int month = 1; month <= 12; month++) {
                List<Map<String, Object>> rows = getMonthData(year, month);
                if (rows.isEmpty()) continue;

                rows.sort(Comparator.comparingInt(r -> {
                    Integer num = (Integer) r.get("personnelNumber");
                    return num != null ? num : Integer.MAX_VALUE;
                }));

                int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
                String monthName = new String[]{"Січень","Лютий","Березень","Квітень","Травень","Червень",
                        "Липень","Серпень","Вересень","Жовтень","Листопад","Грудень"}[month - 1];

                // Аркуш: Графік_Місяць
                XSSFSheet sheet1 = wb.createSheet("Графік " + monthName);
                buildScheduleSheet(sheet1, rows, year, month, daysInMonth);

                // Аркуш: Статистика_Місяць
                XSSFSheet sheet2 = wb.createSheet("Статистика " + monthName);
                buildStatsSheet(sheet2, rows);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ПОБУДОВА АРКУШІВ (спільний код) =====

    private void buildScheduleSheet(XSSFSheet sheet, List<Map<String, Object>> rows, int year, int month, int daysInMonth) {
        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dayNameStyle = createDayNameStyle(sheet.getWorkbook());
        XSSFCellStyle dataCenterStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle dataLeftStyle = createLeftAlignedDataStyle(sheet.getWorkbook());

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

        // Рядок 1: дні тижня
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
                XSSFCellStyle statusStyle = getStatusStyle(statusCode, sheet.getWorkbook());
                setCell(dataRow, 2 + d, display, statusStyle);
            }
        }
    }

    private void buildStatsSheet(XSSFSheet sheet, List<Map<String, Object>> rows) {
        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataCenterStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle dataLeftStyle = createLeftAlignedDataStyle(sheet.getWorkbook());

        String[] categories = {"БЧ", "Бат БЧ", "БЧ РКП", "Р-ня", "В-ня", "В-ка", "Х-й", "СЗЧ", "Плюс", "ППД", "Навчання", "Перевівся"};
        int colCount = 3 + categories.length;
        String[] headers = new String[colCount];
        headers[0] = "№";
        headers[1] = "Звання";
        headers[2] = "ПІБ";
        for (int i = 0; i < categories.length; i++) {
            headers[3 + i] = categories[i];
        }

        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        for (int i = 0; i < categories.length; i++) {
            sheet.setColumnWidth(3 + i, 12 * 256);
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

            Integer personnelNumber = (Integer) rowMap.get("personnelNumber");
            Object num = personnelNumber != null ? personnelNumber : rowNum - 1;
            setCell(dataRow, 0, num, dataCenterStyle);
            setCell(dataRow, 1, rowMap.get("rank"), dataLeftStyle);
            setCell(dataRow, 2, rowMap.get("shortName"), dataLeftStyle);

            @SuppressWarnings("unchecked")
            Map<Integer, String> daysMap = (Map<Integer, String>) rowMap.get("days");
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String cat : categories) counts.put(cat, 0);
            for (String statusCode : daysMap.values()) {
                if (statusCode != null && !statusCode.isEmpty()) {
                    String category = CATEGORY_MAPPING.get(statusCode);
                    if (category != null && counts.containsKey(category)) {
                        counts.put(category, counts.get(category) + 1);
                    }
                }
            }

            int col = 3;
            for (String cat : categories) {
                setCell(dataRow, col++, counts.getOrDefault(cat, 0), dataCenterStyle);
            }
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
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
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