package org.example.controller;

import org.example.service.CombatScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
}