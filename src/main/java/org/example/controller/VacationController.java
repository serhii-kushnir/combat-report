package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.service.VacationService;
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
import java.util.*;

@Controller
@RequestMapping("/vacations")
public class VacationController {

    private static final Logger log = LoggerFactory.getLogger(VacationController.class);
    private final VacationService service;

    public VacationController(VacationService service) {
        this.service = service;
    }

    @GetMapping
    public String vacationsPage() {
        return "vacations";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getYearData(@RequestParam int year) {
        try {
            return ResponseEntity.ok(service.getYearData(year));
        } catch (Exception e) {
            log.error("Помилка отримання даних відпусток: year={}", year, e);
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
    public ResponseEntity<?> getPersonVacations(@PathVariable Long personnelId) {
        try {
            return ResponseEntity.ok(service.getPersonVacations(personnelId));
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
            LocalDate end = LocalDate.parse(body.get("endDate").toString());
            String type = body.get("vacationType") != null ? body.get("vacationType").toString() : null;
            String note = body.get("note") != null ? body.get("note").toString() : null;

            return ResponseEntity.ok(service.create(personnelId, start, end, type, note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка створення відпустки", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long personnelId = Long.valueOf(body.get("personnelId").toString());
            LocalDate start = LocalDate.parse(body.get("startDate").toString());
            LocalDate end = LocalDate.parse(body.get("endDate").toString());
            String type = body.get("vacationType") != null ? body.get("vacationType").toString() : null;
            String note = body.get("note") != null ? body.get("note").toString() : null;

            return ResponseEntity.ok(service.update(id, personnelId, start, end, type, note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка оновлення відпустки id={}", id, e);
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

    // ===== ЕКСПОРТ XLSX =====
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam int year) {
        try {
            List<Map<String, Object>> rows = service.getYearData(year);
            byte[] data = buildXlsx(year, rows);
            String filename = URLEncoder.encode("Відпустки_" + year + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту відпусток", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] buildXlsx(int year, List<Map<String, Object>> rows) throws Exception {
        String[] MONTHS = {"Січ", "Лют", "Бер", "Кві", "Тра", "Чер",
                "Лип", "Сер", "Вер", "Жов", "Лис", "Гру"};
        String[] HEADERS = new String[3 + 12 + 3];
        HEADERS[0] = "№";
        HEADERS[1] = "Звання";
        HEADERS[2] = "ПІБ";
        for (int i = 0; i < 12; i++) HEADERS[3 + i] = MONTHS[i];
        HEADERS[15] = "1 півріччя";
        HEADERS[16] = "2 півріччя";
        HEADERS[17] = "Всього";

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle leftStyle = createLeftStyle(wb);
            CellStyle badgeStyle = createBadgeStyle(wb);
            CellStyle h1HeaderStyle = createColoredHeaderStyle(wb, 21, 101, 192);
            CellStyle h2HeaderStyle = createColoredHeaderStyle(wb, 106, 27, 154);
            CellStyle totalHeaderStyle = createColoredHeaderStyle(wb, 46, 125, 50);

            // ===== АРКУШ 1: ГРАФІК =====
            Sheet sheet = wb.createSheet("Графік " + year);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                if (i == 15) cell.setCellStyle(h1HeaderStyle);
                else if (i == 16) cell.setCellStyle(h2HeaderStyle);
                else if (i == 17) cell.setCellStyle(totalHeaderStyle);
                else cell.setCellStyle(headerStyle);
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

                setCell(r, col++, row.get("h1Days"), centerStyle);
                setCell(r, col++, row.get("h2Days"), centerStyle);
                setCell(r, col++, row.get("totalDays"), centerStyle);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, (i < 3 ? 20 : 8) * 256);
            }

            // ===== АРКУШ 2: ДЕТАЛІ =====
            Sheet detailsSheet = wb.createSheet("Деталі " + year);

            String[] DETAIL_HEADERS = {
                    "№", "Звання", "ПІБ", "Початок", "Кінець", "Днів", "Тип", "Примітка"
            };
            int[] DETAIL_WIDTHS = {8, 20, 30, 14, 14, 10, 26, 40};

            Row detHeader = detailsSheet.createRow(0);
            detHeader.setHeightInPoints(28);
            for (int i = 0; i < DETAIL_HEADERS.length; i++) {
                Cell cell = detHeader.createCell(i);
                cell.setCellValue(DETAIL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                detailsSheet.setColumnWidth(i, DETAIL_WIDTHS[i] * 256);
            }

            // Збираємо всі відпустки
            List<VacationDetail> allDetails = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Integer pNum = (Integer) row.get("personnelNumber");
                String rank = (String) row.get("rank");
                String shortName = (String) row.get("shortName");
                @SuppressWarnings("unchecked")
                List<org.example.entity.Vacation> vacs =
                        (List<org.example.entity.Vacation>) row.get("vacations");
                if (vacs == null) continue;
                for (org.example.entity.Vacation v : vacs) {
                    VacationDetail vd = new VacationDetail();
                    vd.personnelNumber = pNum;
                    vd.rank = rank;
                    vd.shortName = shortName;
                    vd.startDate = v.getStartDate();
                    vd.endDate = v.getEndDate();
                    vd.type = v.getVacationType();
                    vd.note = v.getNote();
                    if (vd.startDate != null && vd.endDate != null) {
                        vd.days = (int) java.time.temporal.ChronoUnit.DAYS.between(vd.startDate, vd.endDate) + 1;
                    }
                    allDetails.add(vd);
                }
            }

            // Сортуємо за ПІБ, потім за датою
            allDetails.sort((a, b) -> {
                String na = a.shortName != null ? a.shortName.toLowerCase() : "";
                String nb = b.shortName != null ? b.shortName.toLowerCase() : "";
                if (!na.equals(nb)) return na.compareTo(nb);
                return a.startDate.compareTo(b.startDate);
            });

            int detRowNum = 1;
            for (VacationDetail vd : allDetails) {
                Row r = detailsSheet.createRow(detRowNum++);
                r.setHeightInPoints(22);
                setCell(r, 0, vd.personnelNumber, centerStyle);
                setCell(r, 1, vd.rank, leftStyle);
                setCell(r, 2, vd.shortName, leftStyle);
                setCell(r, 3, vd.startDate != null ? vd.startDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "", centerStyle);
                setCell(r, 4, vd.endDate != null ? vd.endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "", centerStyle);
                setCell(r, 5, vd.days, centerStyle);
                setCell(r, 6, vd.type, leftStyle);
                setCell(r, 7, vd.note, leftStyle);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // Допоміжний клас для деталей
    private static class VacationDetail {
        Integer personnelNumber;
        String rank;
        String shortName;
        java.time.LocalDate startDate;
        java.time.LocalDate endDate;
        Integer days = 0;
        String type;
        String note;
    }

    // Новий стиль для кольорових заголовків
    private CellStyle createColoredHeaderStyle(Workbook wb, int r, int g, int b) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                new byte[]{(byte)r, (byte)g, (byte)b}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s);
        return s;
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value.toString());
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

    private CellStyle createBadgeStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.BLUE.getIndex());
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