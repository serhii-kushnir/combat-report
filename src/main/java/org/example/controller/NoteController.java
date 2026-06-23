package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.Note;
import org.example.service.NoteService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/notes")
public class NoteController {

    private static final Logger log = LoggerFactory.getLogger(NoteController.class);
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping
    public String notesPage() {
        return "notes";
    }

    @GetMapping("/api")
    @ResponseBody
    public List<Note> getAll() {
        return service.getAllActive();
    }

    @GetMapping("/api/archived")
    @ResponseBody
    public List<Note> getArchived() {
        return service.getAllArchived();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Note> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody Note note) {
        try {
            if (note.getTitle() == null || note.getTitle().isBlank()) {
                return ResponseEntity.badRequest().body("Заголовок не може бути порожнім");
            }
            Note saved = service.save(note);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Помилка створення нотатки", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Note note) {
        try {
            return service.getById(id)
                    .map(existing -> {
                        existing.setTitle(note.getTitle());
                        existing.setContent(note.getContent());
                        existing.setColor(note.getColor());
                        existing.setUpdatedAt(java.time.LocalDateTime.now());
                        return ResponseEntity.ok(service.save(existing));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Помилка оновлення нотатки id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PatchMapping("/api/{id}/pin")
    @ResponseBody
    public ResponseEntity<?> togglePin(@PathVariable Long id) {
        try {
            service.togglePin(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PatchMapping("/api/{id}/archive")
    @ResponseBody
    public ResponseEntity<?> archive(@PathVariable Long id) {
        try {
            service.archive(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PatchMapping("/api/{id}/unarchive")
    @ResponseBody
    public ResponseEntity<?> unarchive(@PathVariable Long id) {
        try {
            service.unarchive(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
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

    @GetMapping("/api/search")
    @ResponseBody
    public List<Note> search(@RequestParam String q) {
        return service.search(q);
    }

    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx() {
        try {
            List<Note> notes = service.getAllActive();
            byte[] data = exportToXlsx(notes);
            String filename = URLEncoder.encode("Нотатки.xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту нотаток", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/export/archived")
    public ResponseEntity<byte[]> exportArchivedXlsx() {
        try {
            List<Note> notes = service.getAllArchived();
            byte[] data = exportToXlsx(notes);
            String filename = URLEncoder.encode("Архів_нотаток.xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту архіву нотаток", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] exportToXlsx(List<Note> notes) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int sheetIndex = 1;
            for (Note note : notes) {
                String sheetName = "Нотатка " + sheetIndex++;
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                XSSFSheet sheet = wb.createSheet(sheetName);

                XSSFCellStyle headerStyle = createHeaderStyle(wb);
                XSSFCellStyle centerStyle = createCenterStyle(wb);
                XSSFCellStyle textLeftStyle = createTextLeftStyle(wb);

                String[] headers = {"№", "Заголовок", "Текст", "Створено", "Оновлено"};
                int[] widths = {6, 30, 60, 18, 18};

                XSSFRow headerRow = sheet.createRow(0);
                headerRow.setHeightInPoints(25);
                for (int i = 0; i < headers.length; i++) {
                    sheet.setColumnWidth(i, widths[i] * 256);
                    XSSFCell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                XSSFRow row = sheet.createRow(1);
                row.setHeight((short)-1);

                setCell(row, 0, 1, centerStyle);
                setCell(row, 1, note.getTitle(), centerStyle);
                setCell(row, 2, note.getContent(), textLeftStyle);
                setCell(row, 3, note.getCreatedAt() != null ? note.getCreatedAt().format(fmt) : "", centerStyle);
                setCell(row, 4, note.getUpdatedAt() != null ? note.getUpdatedAt().format(fmt) : "", centerStyle);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private XSSFCellStyle createTextLeftStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);    // ліворуч
        s.setVerticalAlignment(VerticalAlignment.CENTER); // по центру вертикально
        s.setWrapText(true);                         // перенесення тексту
        setBorders(s);
        return s;
    }

    private XSSFCellStyle createCenterStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(false);
        setBorders(s);
        return s;
    }

    // Для колонки "Текст" – центрування + перенесення
    private XSSFCellStyle createTextStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s);
        return s;
    }

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer) cell.setCellValue((Integer) value);
        else if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value.toString());
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26, (byte)35, (byte)126}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s);
        return s;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.TOP);  // вирівнювання по верхньому краю
        s.setWrapText(true);   // перенесення тексту
        setBorders(s);
        return s;
    }

    private void setBorders(XSSFCellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}