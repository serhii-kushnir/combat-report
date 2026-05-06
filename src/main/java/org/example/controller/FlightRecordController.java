package org.example.controller;

import org.example.entity.FlightRecord;
import org.example.service.FlightRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/flights")
public class FlightRecordController {

    private static final Logger log = LoggerFactory.getLogger(FlightRecordController.class);
    private final FlightRecordService service;

    public FlightRecordController(FlightRecordService service) {
        this.service = service;
    }

    /** Сторінка журналу */
    @GetMapping
    public String flightsPage() {
        return "flights";
    }

    /** REST: всі записи або за місяцем */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getAll(@RequestParam(required = false) String month) {
        try {
            List<FlightRecord> records = (month != null && !month.isBlank())
                    ? service.getByMonth(month)
                    : service.getAll();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("Помилка отримання записів", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    /** REST: список місяців */
    @GetMapping("/api/months")
    @ResponseBody
    public List<String> getMonths() {
        return service.getMonths();
    }

    /** REST: один запис */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** REST: додати запис */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody FlightRecord record) {
        try {
            return ResponseEntity.ok(service.save(record));
        } catch (Exception e) {
            log.error("Помилка додавання запису", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    /** REST: оновити запис */
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FlightRecord record) {
        try {
            record.setId(id);
            return ResponseEntity.ok(service.save(record));
        } catch (Exception e) {
            log.error("Помилка оновлення запису id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    /** REST: видалити запис */
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

    /** Імпорт xlsx */
    @PostMapping("/api/import")
    @ResponseBody
    public ResponseEntity<?> importXlsx(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("Файл порожній");
        try {
            int count = service.importFromXlsx(file);
            return ResponseEntity.ok(Map.of("imported", count));
        } catch (Exception e) {
            log.error("Помилка імпорту xlsx", e);
            return ResponseEntity.internalServerError().body("Помилка імпорту: " + e.getMessage());
        }
    }

    /** Експорт xlsx */
    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String month) {
        try {
            byte[] data = service.exportToXlsx(month);
            String filename = URLEncoder.encode(
                    "Звіт_БпАК" + (month != null ? "_" + month : "") + ".xlsx",
                    StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту xlsx", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
