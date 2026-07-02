package org.example.service;

import org.example.entity.CombatDuty;
import org.example.entity.Personnel;
import org.example.repository.CombatDutyRepository;
import org.example.repository.PersonnelRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CombatScheduleService {

    private final CombatDutyRepository dutyRepo;
    private final PersonnelRepository personnelRepo;

    public CombatScheduleService(CombatDutyRepository dutyRepo, PersonnelRepository personnelRepo) {
        this.dutyRepo = dutyRepo;
        this.personnelRepo = personnelRepo;
    }

    public List<Map<String, Object>> getMonthData(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<CombatDuty> duties = dutyRepo.findAll().stream()
                .filter(d -> !d.getStartTime().toLocalDate().isAfter(to) && !d.getEndTime().toLocalDate().isBefore(from))
                .collect(Collectors.toList());

        List<Personnel> personnel = personnelRepo.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");

        Map<Integer, Map<String, String>> dayRoles = new HashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            dayRoles.put(d, new HashMap<>());
        }

        for (CombatDuty duty : duties) {
            LocalDate start = duty.getStartTime().toLocalDate();
            LocalDate end = duty.getEndTime().toLocalDate();
            LocalDate fromDay = start.isBefore(from) ? from : start;
            LocalDate toDay = end.isAfter(to) ? to : end;

            Map<String, String> roleMap = new HashMap<>();
            addRole(roleMap, duty.getCommander(), "К");
            addRole(roleMap, duty.getPilot(), "П");
            addRole(roleMap, duty.getNavigator(), "Ш");
            addRole(roleMap, duty.getTechnician(), "Т");

            for (LocalDate date = fromDay; !date.isAfter(toDay); date = date.plusDays(1)) {
                int day = date.getDayOfMonth();
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
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Personnel p : personnel) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("shortName", p.getShortName());
            row.put("rank", p.getRank() != null ? p.getRank() : "");
            row.put("personnelNumber", p.getPersonnelNumber());

            Map<Integer, String> days = new LinkedHashMap<>();
            String normalizedFullName = normalizeName(p.getFullName());
            int total = 0;
            for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                Map<String, String> dayMap = dayRoles.get(d);
                String role = dayMap != null ? dayMap.getOrDefault(normalizedFullName, "") : "";
                days.put(d, role);
                if (!role.isEmpty()) total++;
            }
            row.put("days", days);
            row.put("total", total);
            rows.add(row);
        }
        return rows;
    }

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

    private void addRole(Map<String, String> roleMap, String fullNameWithRank, String role) {
        if (fullNameWithRank == null || fullNameWithRank.trim().isEmpty()) return;
        String normalized = normalizeName(fullNameWithRank);
        if (normalized != null && !normalized.isEmpty()) {
            roleMap.put(normalized, role);
        }
    }

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

    private String normalizeName(String name) {
        if (name == null) return null;
        String withoutRank = extractNameWithoutRank(name);
        if (withoutRank == null) return null;
        return withoutRank.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public List<Integer> getYears() {
        int currentYear = LocalDate.now().getYear();
        return List.of(currentYear, currentYear - 1);
    }

    public List<String> getMonths() {
        return List.of("Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень");
    }
}