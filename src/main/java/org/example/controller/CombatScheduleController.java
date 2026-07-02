package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.service.CombatScheduleService;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/combat-schedule")
public class CombatScheduleController {

    private final CombatScheduleService service;

    public CombatScheduleController(CombatScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public String schedulePage() {
        return "combat-schedule";
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

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getStats(@RequestParam int year, @RequestParam int month) {
        try {
            List<Map<String, Object>> data = service.getStats(year, month);
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

    // ===== ЕКСПОРТ XLSX (підтримує місяць або весь рік) =====
    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "num") String sortCol,
            @RequestParam(required = false, defaultValue = "1") int sortDir,
            @RequestParam(required = false, defaultValue = "num") String statsSortCol,
            @RequestParam(required = false, defaultValue = "1") int statsSortDir) {
        try {
            if (month != null) {
                // Експорт за один місяць
                List<Map<String, Object>> rows = service.getMonthData(year, month);
                List<Map<String, Object>> stats = service.getStats(year, month);
                rows.sort(getComparator(sortCol, sortDir));
                stats.sort(getStatsComparator(statsSortCol, statsSortDir));

                byte[] data = exportSingleMonth(year, month, rows, stats);
                String filename = URLEncoder.encode("БЧ_Графік_" + year + "_" + month + ".xlsx", StandardCharsets.UTF_8)
                        .replace("+", "%20");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(data);
            } else {
                // Експорт за весь рік
                byte[] data = exportWholeYear(year, sortCol, sortDir, statsSortCol, statsSortDir);
                String filename = URLEncoder.encode("БЧ_Графік_" + year + ".xlsx", StandardCharsets.UTF_8)
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
    private byte[] exportWholeYear(int year, String sortCol, int sortDir, String statsSortCol, int statsSortDir) throws Exception {
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int month = 1; month <= 12; month++) {
                List<Map<String, Object>> rows = service.getMonthData(year, month);
                List<Map<String, Object>> stats = service.getStats(year, month);
                rows.sort(getComparator(sortCol, sortDir));
                stats.sort(getStatsComparator(statsSortCol, statsSortDir));

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

        // ===== 1. ГРАФІК =====
        int rowNum = 0;

        // Заголовки (1-й рядок – числа)
        Row headerRow1 = sheet.createRow(rowNum++);
        headerRow1.setHeightInPoints(25);
        int col = 0;
        createCell(headerRow1, col++, "№", headerStyle);
        createCell(headerRow1, col++, "Звання", headerStyle);
        createCell(headerRow1, col++, "ПІБ", headerStyle);
        for (int d = 1; d <= daysInMonth; d++) {
            createCell(headerRow1, col++, String.valueOf(d), headerStyle);
        }

        // Заголовки (2-й рядок – дні тижня)
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

        // Дані графіка
        for (Map<String, Object> rowData : rows) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(20);
            col = 0;
            createCell(row, col++, rowData.get("personnelNumber") != null ? rowData.get("personnelNumber") : "", centerStyle);
            createCell(row, col++, rowData.get("rank") != null ? rowData.get("rank") : "", leftStyle);
            createCell(row, col++, rowData.get("shortName") != null ? rowData.get("shortName") : "", leftStyle);

            Map<Integer, String> days = (Map<Integer, String>) rowData.get("days");
            for (int d = 1; d <= daysInMonth; d++) {
                String role = days.getOrDefault(d, "");
                CellStyle style = getRoleStyle(role, sheet.getWorkbook());
                createCell(row, col++, role, style);
            }
        }

        // Ширини для графіка
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        for (int d = 1; d <= daysInMonth; d++) {
            sheet.setColumnWidth(2 + d, 8 * 256);
        }

        // ===== ПРОМІЖОК =====
        rowNum++; // порожній рядок

        // ===== 2. СТАТИСТИКА =====
        Row statTitleRow = sheet.createRow(rowNum++);
        statTitleRow.setHeightInPoints(22);
        createCell(statTitleRow, 0, "📊 Статистика чергувань", createHeaderStyle(sheet.getWorkbook()));

        String[] statHeaders = {"№", "Звання", "ПІБ", "К", "П", "Ш", "Т", "Днів БЧ"};
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
            createCell(row, 3, statRow.get("countK") != null ? statRow.get("countK") : 0, centerStyle);
            createCell(row, 4, statRow.get("countP") != null ? statRow.get("countP") : 0, centerStyle);
            createCell(row, 5, statRow.get("countSh") != null ? statRow.get("countSh") : 0, centerStyle);
            createCell(row, 6, statRow.get("countT") != null ? statRow.get("countT") : 0, centerStyle);
            createCell(row, 7, statRow.get("total") != null ? statRow.get("total") : 0, centerStyle);
        }

        // Автоширина для статистики
        for (int i = 0; i < statHeaders.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 512);
        }
    }

    // ===== КОМПАРАТОРИ =====
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
            if (col.startsWith("day")) {
                int day = Integer.parseInt(col.substring(3));
                Map<Integer, String> daysA = (Map<Integer, String>) a.get("days");
                Map<Integer, String> daysB = (Map<Integer, String>) b.get("days");
                String v1 = daysA != null ? daysA.getOrDefault(day, "") : "";
                String v2 = daysB != null ? daysB.getOrDefault(day, "") : "";
                return v1.compareTo(v2) * dir;
            }
            Integer va = (Integer) a.get("personnelNumber");
            Integer vb = (Integer) b.get("personnelNumber");
            if (va == null) va = Integer.MAX_VALUE;
            if (vb == null) vb = Integer.MAX_VALUE;
            return va.compareTo(vb) * dir;
        };
    }

    private Comparator<Map<String, Object>> getStatsComparator(String col, int dir) {
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
            Integer va = a.get(col) != null ? ((Number) a.get(col)).intValue() : 0;
            Integer vb = b.get(col) != null ? ((Number) b.get(col)).intValue() : 0;
            return va.compareTo(vb) * dir;
        };
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

    private CellStyle getRoleStyle(String role, Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);

        if (role == null || role.isEmpty()) {
            s.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return s;
        }

        if (role.contains("К")) {
            s.setFillForegroundColor(IndexedColors.RED.getIndex());
        } else if (role.contains("П")) {
            s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        } else if (role.contains("Ш")) {
            s.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        } else if (role.contains("Т")) {
            s.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        } else {
            s.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        }
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private void setBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}