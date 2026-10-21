package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.service.VlkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/vlk")
public class VlkController {

    private static final Logger log = LoggerFactory.getLogger(VlkController.class);
    private final VlkService service;

    public VlkController(VlkService service) {
        this.service = service;
    }

    @GetMapping
    public String vlkPage() {
        return "vlk";
    }

    // ===== API: сітка =====
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getGrid(@RequestParam int year) {
        try {
            return ResponseEntity.ok(service.getYearData(year));
        } catch (Exception e) {
            log.error("Помилка отримання сітки ВЛК: year={}", year, e);
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    // ===== API: черга =====
    @GetMapping("/api/queue")
    @ResponseBody
    public ResponseEntity<?> getQueue(@RequestParam(required = false) Integer year) {
        try {
            return ResponseEntity.ok(service.getQueue(year));
        } catch (Exception e) {
            log.error("Помилка отримання черги ВЛК: year={}", year, e);
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/api/years")
    @ResponseBody
    public List<Integer> getYears() {
        return service.getYears();
    }

    @GetMapping("/api/person/{personnelId}")
    @ResponseBody
    public ResponseEntity<?> getPersonVlks(@PathVariable Long personnelId) {
        try {
            return ResponseEntity.ok(service.getPersonVlks(personnelId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Long personnelId = Long.valueOf(body.get("personnelId").toString());
            LocalDate start = LocalDate.parse(body.get("startDate").toString());
            LocalDate end   = LocalDate.parse(body.get("endDate").toString());
            String type = body.get("vlkType") != null ? body.get("vlkType").toString() : null;
            String conclusion = body.get("conclusion") != null ? body.get("conclusion").toString() : null;
            String note = body.get("note") != null ? body.get("note").toString() : null;

            return ResponseEntity.ok(service.create(personnelId, start, end, type, conclusion, note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка створення ВЛК", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long personnelId = Long.valueOf(body.get("personnelId").toString());
            LocalDate start = LocalDate.parse(body.get("startDate").toString());
            LocalDate end   = LocalDate.parse(body.get("endDate").toString());
            String type = body.get("vlkType") != null ? body.get("vlkType").toString() : null;
            String conclusion = body.get("conclusion") != null ? body.get("conclusion").toString() : null;
            String note = body.get("note") != null ? body.get("note").toString() : null;

            return ResponseEntity.ok(service.update(id, personnelId, start, end, type, conclusion, note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка оновлення ВЛК id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    // ============================================================
    //  ЄДИНИЙ ЕКСПОРТ: один файл, два аркуші (Графік + Черга)
    // ============================================================
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAll(@RequestParam int year) {
        try {
            List<Map<String, Object>> gridRows  = service.getYearData(year);
            List<Map<String, Object>> queueRows = service.getQueue(year);

            byte[] data = buildCombinedXlsx(year, gridRows, queueRows);

            String filename = URLEncoder.encode("ВЛК_" + year + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту ВЛК", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] buildCombinedXlsx(int year,
                                     List<Map<String, Object>> gridRows,
                                     List<Map<String, Object>> queueRows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            buildGridSheet(wb, year, gridRows);
            buildQueueSheet(wb, year, queueRows);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== Аркуш 1: Графік =====
    private void buildGridSheet(XSSFWorkbook wb, int year, List<Map<String, Object>> rows) {
        String[] MONTHS = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        String[] HEADERS = new String[3 + 12 + 1];
        HEADERS[0] = "№";
        HEADERS[1] = "Звання";
        HEADERS[2] = "ПІБ";
        for (int i = 0; i < 12; i++) HEADERS[3 + i] = MONTHS[i];
        HEADERS[15] = "Всього";

        CellStyle headerStyle      = createHeaderStyle(wb);
        CellStyle centerStyle      = createCenterStyle(wb);
        CellStyle leftStyle        = createLeftStyle(wb);
        CellStyle badgeStyle       = createBadgeStyle(wb, IndexedColors.TEAL);
        CellStyle totalHeaderStyle = createColoredHeaderStyle(wb, 46, 125, 50);
        CellStyle totalCellStyle   = createColoredCellStyle(wb, 232, 245, 233, 27, 94, 32);

        Sheet sheet = wb.createSheet("Графік " + year);

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(30);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(i == 15 ? totalHeaderStyle : headerStyle);
        }

        int rowNum = 1;
        for (Map<String, Object> row : rows) {
            Row r = sheet.createRow(rowNum++);
            r.setHeightInPoints(22);

            int col = 0;
            setCell(r, col++, row.get("personnelNumber"), centerStyle);
            setCell(r, col++, row.get("rank"), leftStyle);
            setCell(r, col++, row.get("shortName"), leftStyle);

            @SuppressWarnings("unchecked")
            Map<Integer, Integer> months = (Map<Integer, Integer>) row.get("months");
            for (int m = 1; m <= 12; m++) {
                Integer days = months.getOrDefault(m, 0);
                Cell c = r.createCell(col++);
                if (days != null && days > 0) {
                    c.setCellValue(days);
                    c.setCellStyle(badgeStyle);
                } else {
                    c.setCellValue("");
                    c.setCellStyle(centerStyle);
                }
            }
            setCell(r, col++, row.get("totalDays"), totalCellStyle);
        }

        for (int i = 0; i < HEADERS.length; i++) {
            int width;
            if (i < 3) width = 20;
            else if (i < 15) width = 12;    // місяці
            else width = 10;                 // "Всього"
            sheet.setColumnWidth(i, width * 256);
        }
    }

    // ===== Аркуш 2: Черга =====
    private void buildQueueSheet(XSSFWorkbook wb, int year, List<Map<String, Object>> rows) {
        CellStyle headerStyle   = createHeaderStyle(wb);
        CellStyle centerStyle   = createCenterStyle(wb);
        CellStyle leftStyle     = createLeftStyle(wb);
        CellStyle upcomingStyle = createBadgeStyle(wb, IndexedColors.BLUE);
        CellStyle activeStyle   = createBadgeStyle(wb, IndexedColors.ORANGE);
        CellStyle pastStyle     = createBadgeStyle(wb, IndexedColors.GREY_50_PERCENT);

        Sheet sheet = wb.createSheet("Черга " + year);

        String[] headers = {
                "Черга", "№", "Звання", "ПІБ", "Початок", "Кінець", "Днів",
                "Тип ВЛК", "Заключення", "Статус", "Примітка"
        };
        int[] widths = {8, 8, 20, 30, 14, 14, 8, 22, 22, 16, 40};

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Map<String, Object> row : rows) {
            Row r = sheet.createRow(rowNum++);
            r.setHeightInPoints(22);

            String status = (String) row.get("status");
            CellStyle statusStyle;
            String statusText;
            if ("active".equals(status))        { statusStyle = activeStyle;   statusText = "Триває"; }
            else if ("upcoming".equals(status)) { statusStyle = upcomingStyle; statusText = "Майбутній"; }
            else                                { statusStyle = pastStyle;     statusText = "Завершено"; }

            setCell(r, 0, row.get("queueNum"), centerStyle);
            setCell(r, 1, row.get("personnelNumber"), centerStyle);
            setCell(r, 2, row.get("rank"), leftStyle);
            setCell(r, 3, row.get("shortName"), leftStyle);
            setCell(r, 4, fmtDate((LocalDate) row.get("startDate")), centerStyle);
            setCell(r, 5, fmtDate((LocalDate) row.get("endDate")), centerStyle);
            setCell(r, 6, row.get("days"), centerStyle);
            setCell(r, 7, row.get("vlkType"), leftStyle);
            setCell(r, 8, row.get("conclusion"), leftStyle);
            setCell(r, 9, statusText, statusStyle);
            setCell(r, 10, row.get("note"), leftStyle);
        }
    }

    // ===== Допоміжні =====
    private String fmtDate(LocalDate d) {
        if (d == null) return "";
        return d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value.toString());
    }

    private XSSFCellStyle createColoredHeaderStyle(XSSFWorkbook wb, int r, int g, int b) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) r, (byte) g, (byte) b}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s);
        return s;
    }

    private XSSFCellStyle createColoredCellStyle(XSSFWorkbook wb,
                                                 int bgR, int bgG, int bgB,
                                                 int fR, int fG, int fB) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(new byte[]{(byte) fR, (byte) fG, (byte) fB}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) bgR, (byte) bgG, (byte) bgB}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
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
        s.setWrapText(true);
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

    private CellStyle createBadgeStyle(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
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