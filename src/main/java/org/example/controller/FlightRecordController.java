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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
    public ResponseEntity<?> getAll(@RequestParam(required = false) String month,
                                    @RequestParam(required = false) Integer year) {
        try {
            List<FlightRecord> records;
            if (month != null && !month.isBlank()) {
                records = service.getByMonth(month);
            } else if (year != null) {
                records = service.getByYear(year);
            } else {
                records = service.getAll();
            }
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

    /** REST: список років */
    @GetMapping("/api/years")
    @ResponseBody
    public List<Integer> getYears() {
        return service.getYears();
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

    /** Експорт xlsx */
    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String month,
                                             @RequestParam(required = false) Integer year) {
        try {
            byte[] data = service.exportToXlsx(month, year);
            String suffix = month != null ? "_" + month : (year != null ? "_" + year : "");
            String filename = URLEncoder.encode(
                    "Звіт_БпАК" + suffix + ".xlsx",
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