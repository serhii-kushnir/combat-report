package org.example.controller;

import org.example.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

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
