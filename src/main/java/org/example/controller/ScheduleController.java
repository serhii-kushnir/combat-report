package org.example.controller;

import org.example.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);
    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    /** Сторінка графіку */
    @GetMapping
    public String schedulePage() {
        return "schedule";
    }

    /** REST: дані за місяць */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getMonth(@RequestParam int year,
                                      @RequestParam int month) {
        try {
            YearMonth.of(year, month); // валідація
            List<Map<String, Object>> data = service.getMonthData(year, month);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Помилка отримання графіку: year={}, month={}", year, month, e);
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToXlsx(@RequestParam(required = false) Integer year,
                                               @RequestParam(required = false) Integer month) {
        try {
            // Якщо рік не вказано, використовуємо поточний
            if (year == null) {
                year = LocalDate.now().getYear();
            }
            byte[] data = service.exportToXlsx(year, month);
            String suffix = (month != null) ? String.format("_%d_%d", year, month) : String.format("_%d", year);
            String filename = URLEncoder.encode(
                    "Графік_чергувань" + suffix + ".xlsx",
                    StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту графіка", e);
            return ResponseEntity.internalServerError().build();
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

    /** REST: встановити статус на клітинку */
    @PostMapping("/api/cell")
    @ResponseBody
    public ResponseEntity<?> setCell(@RequestBody Map<String, Object> body) {
        try {
            Long personnelId = Long.valueOf(body.get("personnelId").toString());
            LocalDate date   = LocalDate.parse(body.get("date").toString());
            String status    = body.getOrDefault("status", "").toString();

            service.setStatus(personnelId, date, status);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка оновлення клітинки графіку", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    /** REST: підрахунок статусів за місяць */
    @GetMapping("/api/counts")
    @ResponseBody
    public ResponseEntity<?> getCounts(@RequestParam int year,
                                       @RequestParam int month) {
        try {
            return ResponseEntity.ok(service.getMonthlyCounts(year, month));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }
}
