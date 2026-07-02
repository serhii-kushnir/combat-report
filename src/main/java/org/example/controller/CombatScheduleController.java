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

    // ===== ЕКСПОРТ XLSX =====
    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam int year, @RequestParam int month) {
        try {
            List<Map<String, Object>> rows = service.getMonthData(year, month);
            List<Map<String, Object>> stats = service.getStats(year, month);
            byte[] data = exportToXlsx(year, month, rows, stats);
            String filename = URLEncoder.encode("БЧ_Графік_" + year + "_" + month + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] exportToXlsx(int year, int month, List<Map<String, Object>> rows, List<Map<String, Object>> stats) throws Exception {
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ===== АРКУШ 1: ГРАФІК (БЕЗ КОЛОНКИ "Днів БЧ") =====
            Sheet sheet1 = wb.createSheet("Графік");
            sheet1.setDefaultRowHeightInPoints(20);

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle leftStyle = createLeftStyle(wb);

            // Заголовки
            Row headerRow1 = sheet1.createRow(0);
            headerRow1.setHeightInPoints(25);
            int col = 0;
            createCell(headerRow1, col++, "№", headerStyle);
            createCell(headerRow1, col++, "Звання", headerStyle);
            createCell(headerRow1, col++, "ПІБ", headerStyle);
            for (int d = 1; d <= daysInMonth; d++) {
                createCell(headerRow1, col++, String.valueOf(d), headerStyle);
            }
            // Без "Днів БЧ"

            // Дні тижня (другий рядок)
            Row headerRow2 = sheet1.createRow(1);
            headerRow2.setHeightInPoints(16);
            String[] daysOfWeek = {"Нд","Пн","Вт","Ср","Чт","Пт","Сб"};
            col = 0;
            createCell(headerRow2, col++, "", headerStyle);
            createCell(headerRow2, col++, "", headerStyle);
            createCell(headerRow2, col++, "", headerStyle);
            for (int d = 1; d <= daysInMonth; d++) {
                int dow = YearMonth.of(year, month).atDay(d).getDayOfWeek().getValue() % 7;
                createCell(headerRow2, col++, daysOfWeek[dow], headerStyle);
            }

            // Дані
            int rowNum = 2;
            for (Map<String, Object> rowData : rows) {
                Row row = sheet1.createRow(rowNum++);
                row.setHeightInPoints(20);
                col = 0;
                createCell(row, col++, rowData.get("personnelNumber") != null ? rowData.get("personnelNumber") : "", centerStyle);
                createCell(row, col++, rowData.get("rank") != null ? rowData.get("rank") : "", leftStyle);
                createCell(row, col++, rowData.get("shortName") != null ? rowData.get("shortName") : "", leftStyle);

                Map<Integer, String> days = (Map<Integer, String>) rowData.get("days");
                for (int d = 1; d <= daysInMonth; d++) {
                    String role = days.getOrDefault(d, "");
                    CellStyle style = getRoleStyle(role, wb);
                    createCell(row, col++, role, style);
                }
                // без колонки total
            }

            // Ширини колонок
            sheet1.setColumnWidth(0, 6 * 256);
            sheet1.setColumnWidth(1, 18 * 256);
            sheet1.setColumnWidth(2, 24 * 256);
            for (int d = 1; d <= daysInMonth; d++) {
                sheet1.setColumnWidth(2 + d, 8 * 256);
            }

            // ===== АРКУШ 2: СТАТИСТИКА =====
            Sheet sheet2 = wb.createSheet("Статистика");
            sheet2.setDefaultRowHeightInPoints(20);

            String[] statHeaders = {"№", "Звання", "ПІБ", "К", "П", "Ш", "Т", "Загалом"};
            Row statHeaderRow = sheet2.createRow(0);
            statHeaderRow.setHeightInPoints(25);
            for (int i = 0; i < statHeaders.length; i++) {
                createCell(statHeaderRow, i, statHeaders[i], headerStyle);
                sheet2.setColumnWidth(i, (statHeaders[i].length() + 4) * 256);
            }

            int statRowNum = 1;
            for (Map<String, Object> statRow : stats) {
                Row row = sheet2.createRow(statRowNum++);
                row.setHeightInPoints(20);
                createCell(row, 0, statRow.get("personnelNumber") != null ? statRow.get("personnelNumber") : "", centerStyle);
                createCell(row, 1, statRow.get("rank") != null ? statRow.get("rank") : "", leftStyle);
                createCell(row, 2, statRow.get("shortName") != null ? statRow.get("shortName") : "", leftStyle);
                createCell(row, 3, statRow.get("countK") != null ? statRow.get("countK") : 0, centerStyle);
                createCell(row, 4, statRow.get("countP") != null ? statRow.get("countP") : 0, centerStyle);
                createCell(row, 5, statRow.get("countSh") != null ? statRow.get("countSh") : 0, centerStyle);
                createCell(row, 6, statRow.get("countT") != null ? statRow.get("countT") : 0, centerStyle);
                createCell(row, 7, statRow.get("total") != null ? statRow.get("total") : 0, centerStyle);
            }

            wb.write(out);
            return out.toByteArray();
        }
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