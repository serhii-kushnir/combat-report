package org.example.controller;

import org.example.service.PpdScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ppd-schedule")
public class PpdScheduleController {

    private static final Logger log = LoggerFactory.getLogger(PpdScheduleController.class);

    private final PpdScheduleService service;

    public PpdScheduleController(PpdScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public String ppdSchedulePage() {
        return "ppd-schedule";
    }

    @PostMapping("/api/cell")
    @ResponseBody
    public ResponseEntity<?> setStatus(@RequestBody Map<String, Object> payload) {
        try {
            Long personnelId = Long.valueOf(payload.get("personnelId").toString());
            LocalDate date = LocalDate.parse(payload.get("date").toString());
            String status = (String) payload.get("status");
            service.setStatus(personnelId, date, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
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

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getStats(@RequestParam int year, @RequestParam int month) {
        try {
            List<Map<String, Object>> data = service.getMonthData(year, month);
            List<Map<String, Object>> stats = new ArrayList<>();
            for (Map<String, Object> row : data) {
                Map<String, Object> statRow = new LinkedHashMap<>();
                statRow.put("id", row.get("id"));
                statRow.put("shortName", row.get("shortName"));
                statRow.put("rank", row.get("rank"));
                statRow.put("personnelNumber", row.get("personnelNumber"));
                Map<Integer, String> days = (Map<Integer, String>) row.get("days");
                int total = 0;
                for (String status : days.values()) {
                    if ("ППД".equals(status)) total++;
                }
                statRow.put("total", total);
                stats.add(statRow);
            }
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam int year,
                                             @RequestParam(required = false) Integer month) {
        try {
            byte[] data;
            String filename;
            if (month == null) {
                // Експортуємо всі місяці року
                data = service.exportFullYearToXlsx(year);
                filename = URLEncoder.encode("Графік_ППД_" + year + ".xlsx", StandardCharsets.UTF_8)
                        .replace("+", "%20");
            } else {
                data = service.exportToXlsx(year, month);
                filename = URLEncoder.encode("Графік_ППД_" + year + "_" + month + ".xlsx", StandardCharsets.UTF_8)
                        .replace("+", "%20");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту ППД", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}