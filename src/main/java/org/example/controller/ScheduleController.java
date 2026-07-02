package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.service.ScheduleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public String schedulePage() {
        return "schedule";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getMonthData(@RequestParam int year, @RequestParam int month) {
        try {
            List<Map<String, Object>> data = service.getMonthData(year, month);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/api/years")
    @ResponseBody
    public List<Integer> getYears() {
        return service.getYears();
    }

    @GetMapping("/api/months")
    @ResponseBody
    public List<String> getMonths() {
        return service.getMonths();
    }

    // ===== ЕКСПОРТ XLSX (з підтримкою сортування) =====
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "name") String sortCol,
            @RequestParam(required = false, defaultValue = "1") int sortDir) {
        try {
            if (month != null) {
                List<Map<String, Object>> rows = service.getMonthData(year, month);
                rows.sort(getComparator(sortCol, sortDir));
                List<Map<String, Object>> stats = buildStatsFromRows(rows);
                byte[] data = exportSingleMonth(year, month, rows, stats);
                String filename = URLEncoder.encode("Графік_чергувань_" + year + "_" + month + ".xlsx", StandardCharsets.UTF_8)
                        .replace("+", "%20");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(data);
            } else {
                byte[] data = exportWholeYear(year, sortCol, sortDir);
                String filename = URLEncoder.encode("Графік_чергувань_" + year + ".xlsx", StandardCharsets.UTF_8)
                        .replace("+", "%20");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== КОМПАРАТОР ДЛЯ СОРТУВАННЯ ГРАФІКА =====
    private Comparator<Map<String, Object>> getComparator(String col, int dir) {
        return (a, b) -> {
            if ("num".equals(col)) {
                Integer va = (Integer) a.get("personnelNumber");
                Integer vb = (Integer) b.get("personnelNumber");
                if (va == null) va = Integer.MAX_VALUE;
                if (vb == null) vb = Integer.MAX_VALUE;
                return va.compareTo(vb) * dir;
            }
            if ("rank".equals(col) || "name".equals(col)) {
                String va = a.get(col) != null ? a.get(col).toString().toLowerCase() : "";
                String vb = b.get(col) != null ? b.get(col).toString().toLowerCase() : "";
                return va.compareTo(vb) * dir;
            }
            Integer va = (Integer) a.get("personnelNumber");
            Integer vb = (Integer) b.get("personnelNumber");
            if (va == null) va = Integer.MAX_VALUE;
            if (vb == null) vb = Integer.MAX_VALUE;
            return va.compareTo(vb) * dir;
        };
    }

    // ===== ПОБУДОВА СТАТИСТИКИ З РЯДКІВ =====
    private List<Map<String, Object>> buildStatsFromRows(List<Map<String, Object>> rows) {
        Map<String, String> categoryMapping = new HashMap<>();
        categoryMapping.put("БЧ", "БЧ");
        categoryMapping.put("БЧ БАТ", "Бат БЧ");
        categoryMapping.put("БЧ РКП", "БЧ РКП");
        categoryMapping.put("Р-НЯ", "Р-ня");
        categoryMapping.put("В-НЯ", "В-ня");
        categoryMapping.put("В-КА", "В-ка");
        categoryMapping.put("ХВ", "Х-й");
        categoryMapping.put("СЗЧ", "СЗЧ");
        categoryMapping.put("ПЛЮ", "Плюс");
        categoryMapping.put("ППД", "ППД");
        categoryMapping.put("НАВ", "Навчання");
        categoryMapping.put("ПЕРЕВ", "Перевівся");

        String[] categories = {"БЧ", "Бат БЧ", "БЧ РКП", "Р-ня", "В-ня", "В-ка", "Х-й", "СЗЧ", "Плюс", "ППД", "Навчання", "Перевівся"};

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> statRow = new LinkedHashMap<>(row);
            Map<Integer, String> days = (Map<Integer, String>) row.get("days");
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String cat : categories) counts.put(cat, 0);
            for (String statusCode : days.values()) {
                if (statusCode != null && !statusCode.isEmpty()) {
                    String category = categoryMapping.get(statusCode);
                    if (category != null && counts.containsKey(category)) {
                        counts.put(category, counts.get(category) + 1);
                    }
                }
            }
            for (String cat : categories) {
                statRow.put(cat, counts.get(cat));
            }
            stats.add(statRow);
        }
        return stats;
    }

    // ===== ЕКСПОРТ ОДНОГО МІСЯЦЯ =====
    private byte[] exportSingleMonth(int year, int month, List<Map<String, Object>> rows, List<Map<String, Object>> stats) throws Exception {
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(monthNames[month - 1]);
            buildMonthSheet(sheet, year, month, rows, stats);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ЕКСПОРТ ВСЬОГО РОКУ (12 АРКУШІВ) =====
    private byte[] exportWholeYear(int year, String sortCol, int sortDir) throws Exception {
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int month = 1; month <= 12; month++) {
                List<Map<String, Object>> rows = service.getMonthData(year, month);
                rows.sort(getComparator(sortCol, sortDir));
                List<Map<String, Object>> stats = buildStatsFromRows(rows);
                Sheet sheet = wb.createSheet(monthNames[month - 1]);
                buildMonthSheet(sheet, year, month, rows, stats);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ПОБУДОВА ОДНОГО АРКУША (ГРАФІК + СТАТИСТИКА) =====
    private void buildMonthSheet(Sheet sheet, int year, int month, List<Map<String, Object>> rows, List<Map<String, Object>> stats) {
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();

        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle centerStyle = createCenterStyle(sheet.getWorkbook());
        CellStyle leftStyle = createLeftStyle(sheet.getWorkbook());

        // ===== ГРАФІК =====
        int rowNum = 0;

        Row headerRow1 = sheet.createRow(rowNum++);
        headerRow1.setHeightInPoints(25);
        int col = 0;
        createCell(headerRow1, col++, "№", headerStyle);
        createCell(headerRow1, col++, "Звання", headerStyle);
        createCell(headerRow1, col++, "ПІБ", headerStyle);
        for (int d = 1; d <= daysInMonth; d++) {
            createCell(headerRow1, col++, String.valueOf(d), headerStyle);
        }

        Row headerRow2 = sheet.createRow(rowNum++);
        headerRow2.setHeightInPoints(16);
        String[] daysOfWeek = {"Нд", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        col = 0;
        createCell(headerRow2, col++, "", headerStyle);
        createCell(headerRow2, col++, "", headerStyle);
        createCell(headerRow2, col++, "", headerStyle);
        for (int d = 1; d <= daysInMonth; d++) {
            int dow = YearMonth.of(year, month).atDay(d).getDayOfWeek().getValue() % 7;
            createCell(headerRow2, col++, daysOfWeek[dow], headerStyle);
        }

        for (Map<String, Object> rowData : rows) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(20);
            col = 0;
            createCell(row, col++, rowData.get("personnelNumber") != null ? rowData.get("personnelNumber") : "", centerStyle);
            createCell(row, col++, rowData.get("rank") != null ? rowData.get("rank") : "", leftStyle);
            createCell(row, col++, rowData.get("shortName") != null ? rowData.get("shortName") : "", leftStyle);

            Map<Integer, String> days = (Map<Integer, String>) rowData.get("days");
            for (int d = 1; d <= daysInMonth; d++) {
                String status = days.getOrDefault(d, "");
                CellStyle style = getStatusStyle(status, sheet.getWorkbook());
                createCell(row, col++, status, style);
            }
        }

        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        for (int d = 1; d <= daysInMonth; d++) {
            sheet.setColumnWidth(2 + d, 10 * 256);
        }

        // ===== СТАТИСТИКА =====
        rowNum++;

        Row statTitleRow = sheet.createRow(rowNum++);
        statTitleRow.setHeightInPoints(22);
        createCell(statTitleRow, 0, "📊 Статистика за категоріями", createHeaderStyle(sheet.getWorkbook()));

        String[] statHeaders = {"№", "Звання", "ПІБ", "БЧ", "Бат БЧ", "БЧ РКП", "Р-ня", "В-ня", "В-ка", "Х-й", "СЗЧ", "Плюс", "ППД", "Навчання", "Перевівся"};
        Row statHeaderRow = sheet.createRow(rowNum++);
        statHeaderRow.setHeightInPoints(25);
        for (int i = 0; i < statHeaders.length; i++) {
            createCell(statHeaderRow, i, statHeaders[i], headerStyle);
        }

        for (Map<String, Object> statRow : stats) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(20);
            createCell(row, 0, statRow.get("personnelNumber") != null ? statRow.get("personnelNumber") : "", centerStyle);
            createCell(row, 1, statRow.get("rank") != null ? statRow.get("rank") : "", centerStyle);
            createCell(row, 2, statRow.get("shortName") != null ? statRow.get("shortName") : "", centerStyle);
            String[] cats = {"БЧ", "Бат БЧ", "БЧ РКП", "Р-ня", "В-ня", "В-ка", "Х-й", "СЗЧ", "Плюс", "ППД", "Навчання", "Перевівся"};
            int cidx = 3;
            for (String cat : cats) {
                createCell(row, cidx++, statRow.get(cat) != null ? statRow.get(cat) : 0, centerStyle);
            }
        }

        for (int i = 0; i < statHeaders.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 512);
        }
    }

    // ===== СТИЛІ ДЛЯ СТАТУСІВ =====
    private CellStyle getStatusStyle(String status, Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);

        if (status == null || status.isEmpty()) {
            s.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return s;
        }

        Map<String, IndexedColors> colorMap = new HashMap<>();
        colorMap.put("БЧ", IndexedColors.RED);
        colorMap.put("БЧ БАТ", IndexedColors.RED);
        colorMap.put("БЧ РКП", IndexedColors.RED);
        colorMap.put("Р-НЯ", IndexedColors.LIGHT_GREEN);
        colorMap.put("В-НЯ", IndexedColors.LIGHT_BLUE);
        colorMap.put("В-КА", IndexedColors.LIGHT_BLUE);
        colorMap.put("ХВ", IndexedColors.PINK);
        colorMap.put("СЗЧ", IndexedColors.ORANGE);
        colorMap.put("ПЛЮ", IndexedColors.VIOLET);
        colorMap.put("ППД", IndexedColors.LIGHT_GREEN);
        colorMap.put("НАВ", IndexedColors.YELLOW);
        colorMap.put("ПЕРЕВ", IndexedColors.GREY_25_PERCENT);

        IndexedColors color = colorMap.get(status);
        if (color != null) {
            s.setFillForegroundColor(color.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } else {
            s.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return s;
    }

    // ===== ДОПОМІЖНІ МЕТОДИ =====
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

    private void setBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}