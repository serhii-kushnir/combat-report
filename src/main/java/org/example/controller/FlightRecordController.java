package org.example.controller;

import org.example.entity.FlightRecord;
import org.example.service.FlightRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public String flightsPage() {
        return "flights";
    }

    // ===== API З ПАГІНАЦІЄЮ =====
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getAll(@RequestParam(required = false) String month,
                                    @RequestParam(required = false) Integer year,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        try {
            Page<FlightRecord> records;
            if (month != null && !month.isBlank()) {
                records = service.getByMonth(month, page, size);
            } else if (year != null) {
                records = service.getByYear(year, page, size);
            } else {
                records = service.getAll(page, size);
            }
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error("Помилка отримання записів", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/api/months")
    @ResponseBody
    public List<String> getMonths() {
        return service.getMonths();
    }

    @GetMapping("/api/years")
    @ResponseBody
    public List<Integer> getYears() {
        return service.getYears();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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

    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String month,
                                             @RequestParam(required = false) Integer year) {
        try {
            byte[] data = service.exportToXlsx(month, year);
            String suffix = month != null ? "_" + month : (year != null ? "_" + year : "");
            String filename = URLEncoder.encode("Звіт_БпАК" + suffix + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
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

    // ===== НОВИЙ ЕНДПОІНТ ДЛЯ ЗАГАЛЬНОЇ СТАТИСТИКИ =====
    @GetMapping("/api/general-stats")
    @ResponseBody
    public Map<String, Long> getGeneralStats() {
        return service.getGeneralStats();
    }
}