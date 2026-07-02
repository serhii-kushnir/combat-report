package org.example.service;

import org.example.entity.CombatDuty;
import org.example.entity.Personnel;
import org.example.repository.CombatDutyRepository;
import org.example.repository.PersonnelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CombatScheduleService {

    private static final Logger log = LoggerFactory.getLogger(CombatScheduleService.class);

    private final CombatDutyRepository dutyRepo;
    private final PersonnelRepository personnelRepo;

    public CombatScheduleService(CombatDutyRepository dutyRepo, PersonnelRepository personnelRepo) {
        this.dutyRepo = dutyRepo;
        this.personnelRepo = personnelRepo;
    }

    /**
     * Отримує дані для місячного графіка чергувань.
     * Повертає список рядків, де кожен рядок – одна особа,
     * а в полі "days" – мапа день → роль ("К", "П", "Ш", "Т" або порожньо).
     */
    public List<Map<String, Object>> getMonthData(int year, int month) {
        log.info("getMonthData: year={}, month={}", year, month);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        // Перетворюємо на LocalDateTime для коректного порівняння
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59);

        // ===== ОПТИМІЗОВАНИЙ ЗАПИТ =====
        // Замість dutyRepo.findAll() використовуємо новий метод
        List<CombatDuty> duties = dutyRepo.findOverlapping(fromDateTime, toDateTime);

        // Всі активні особи
        List<Personnel> personnel = personnelRepo.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");

        // Мапа: день -> (ПІБ -> роль)
        Map<Integer, Map<String, String>> dayRoles = new HashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            dayRoles.put(d, new HashMap<>());
        }

        for (CombatDuty duty : duties) {
            LocalDate startDate = duty.getStartTime().toLocalDate();

            // Якщо дата початку не належить до вибраного місяця – пропускаємо
            if (startDate.isBefore(from) || startDate.isAfter(to)) {
                continue;
            }

            int day = startDate.getDayOfMonth();

            Map<String, String> roleMap = new HashMap<>();
            addRole(roleMap, duty.getCommander(), "К");
            addRole(roleMap, duty.getPilot(), "П");
            addRole(roleMap, duty.getNavigator(), "Ш");
            addRole(roleMap, duty.getTechnician(), "Т");

            Map<String, String> currentDayRoles = dayRoles.get(day);
            for (Map.Entry<String, String> entry : roleMap.entrySet()) {
                String key = entry.getKey();
                String existing = currentDayRoles.get(key);
                if (existing == null) {
                    currentDayRoles.put(key, entry.getValue());
                } else if (!existing.equals(entry.getValue())) {
                    currentDayRoles.put(key, existing + "/" + entry.getValue());
                }
            }
        }

        // Формуємо вихідні рядки
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Personnel p : personnel) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("shortName", p.getShortName());
            row.put("rank", p.getRank() != null ? p.getRank() : "");
            row.put("personnelNumber", p.getPersonnelNumber());

            Map<Integer, String> days = new LinkedHashMap<>();
            String normalizedFullName = normalizeName(p.getFullName());
            int count = 0;
            for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                Map<String, String> dayMap = dayRoles.get(d);
                String role = dayMap != null ? dayMap.getOrDefault(normalizedFullName, "") : "";
                days.put(d, role);
                if (!role.isEmpty()) count++;
            }
            row.put("days", days);
            row.put("count", count);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Отримує статистику: кількість днів за кожною роллю для кожної особи.
     */
    public List<Map<String, Object>> getStats(int year, int month) {
        List<Map<String, Object>> monthData = getMonthData(year, month);
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map<String, Object> row : monthData) {
            Map<String, Object> statRow = new LinkedHashMap<>();
            statRow.put("id", row.get("id"));
            statRow.put("shortName", row.get("shortName"));
            statRow.put("rank", row.get("rank"));
            statRow.put("personnelNumber", row.get("personnelNumber"));

            Map<Integer, String> days = (Map<Integer, String>) row.get("days");
            int countK = 0, countP = 0, countSh = 0, countT = 0;
            for (String role : days.values()) {
                if (role.contains("К")) countK++;
                if (role.contains("П")) countP++;
                if (role.contains("Ш")) countSh++;
                if (role.contains("Т")) countT++;
            }
            statRow.put("countK", countK);
            statRow.put("countP", countP);
            statRow.put("countSh", countSh);
            statRow.put("countT", countT);
            statRow.put("total", countK + countP + countSh + countT);
            stats.add(statRow);
        }
        return stats;
    }

    // ===== ДОПОМІЖНІ МЕТОДИ =====

    private void addRole(Map<String, String> roleMap, String fullNameWithRank, String role) {
        if (fullNameWithRank == null || fullNameWithRank.trim().isEmpty()) return;
        String normalized = normalizeName(fullNameWithRank);
        if (normalized != null && !normalized.isEmpty()) {
            roleMap.put(normalized, role);
        }
    }

    /**
     * Видаляє з рядка поширені військові звання.
     */
    private String extractNameWithoutRank(String fullNameWithRank) {
        String[] ranks = {
                "солдат", "старший солдат", "молодший сержант", "сержант", "старший сержант",
                "головний сержант", "штаб-сержант", "майстер-сержант",
                "молодший лейтенант", "лейтенант", "старший лейтенант", "капітан",
                "майор", "підполковник", "полковник",
                "ст. сержант", "ст. солдат", "ст. лейтенант", "ст. прапорщик",
                "прапорщик", "старший прапорщик",
                "рядовий", "єфрейтор"
        };
        String trimmed = fullNameWithRank.trim();
        for (String rank : ranks) {
            if (trimmed.startsWith(rank + " ")) {
                return trimmed.substring(rank.length() + 1).trim();
            }
        }
        return trimmed;
    }

    /**
     * Нормалізує ПІБ: видаляє звання, зайві пробіли, приводить до нижнього регістру.
     */
    private String normalizeName(String name) {
        if (name == null) return null;
        String withoutRank = extractNameWithoutRank(name);
        if (withoutRank == null) return null;
        return withoutRank.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // ===== МЕТОДИ ДЛЯ ФІЛЬТРІВ =====

    public List<Integer> getYears() {
        return dutyRepo.findDistinctYears();
    }

    public List<String> getMonths() {
        List<Integer> monthNumbers = dutyRepo.findDistinctMonths();
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};
        return monthNumbers.stream()
                .map(num -> monthNames[num - 1])
                .collect(Collectors.toList());
    }
}