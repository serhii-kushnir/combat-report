package org.example.service;

import org.example.entity.Personnel;
import org.example.entity.Vacation;
import org.example.repository.PersonnelRepository;
import org.example.repository.VacationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class VacationService {

    private static final Logger log = LoggerFactory.getLogger(VacationService.class);

    private final VacationRepository vacationRepo;
    private final PersonnelRepository personnelRepo;

    public VacationService(VacationRepository vacationRepo,
                           PersonnelRepository personnelRepo) {
        this.vacationRepo = vacationRepo;
        this.personnelRepo = personnelRepo;
    }

    // ============================================================
    //  МІСЯЧНА СІТКА (як було)
    // ============================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getYearData(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<Personnel> personnel = personnelRepo
                .findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        List<Vacation> allVacations = vacationRepo.findByYear(yearStart, yearEnd);

        Map<Long, List<Vacation>> byPerson = new HashMap<>();
        for (Vacation v : allVacations) {
            Long pid = v.getPersonnel() != null ? v.getPersonnel().getId() : null;
            if (pid != null) {
                byPerson.computeIfAbsent(pid, k -> new ArrayList<>()).add(v);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Personnel p : personnel) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("shortName", p.getShortName());
            row.put("rank", p.getRank() != null ? p.getRank() : "");
            row.put("personnelNumber", p.getPersonnelNumber());

            List<Vacation> personVacations = byPerson.getOrDefault(p.getId(), Collections.emptyList());

            Map<Integer, Integer> monthDays = new LinkedHashMap<>();
            for (int m = 1; m <= 12; m++) monthDays.put(m, 0);

            int h1Days = 0;
            int h2Days = 0;
            int totalDays = 0;

            for (Vacation v : personVacations) {
                LocalDate cursor = v.getStartDate().isBefore(yearStart) ? yearStart : v.getStartDate();
                LocalDate end = v.getEndDate().isAfter(yearEnd) ? yearEnd : v.getEndDate();

                while (!cursor.isAfter(end)) {
                    int month = cursor.getMonthValue();
                    monthDays.merge(month, 1, Integer::sum);
                    if (month <= 6) h1Days++;
                    else h2Days++;
                    totalDays++;
                    cursor = cursor.plusDays(1);
                }
            }

            row.put("months", monthDays);
            row.put("h1Days", h1Days);
            row.put("h2Days", h2Days);
            row.put("totalDays", totalDays);
            row.put("vacations", personVacations);
            rows.add(row);
        }
        return rows;
    }

    // ============================================================
    //  ЧЕРГА ВІДПУСТОК — "хто пройшов, хто наступний"
    // ============================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQueue(Integer year) {
        List<Vacation> vacations;
        if (year != null) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd   = LocalDate.of(year, 12, 31);
            vacations = vacationRepo.findByYear(yearStart, yearEnd);
        } else {
            vacations = vacationRepo.findAllWithPersonnel();
        }

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Vacation v : vacations) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.getId());
            row.put("personnelId", v.getPersonnel().getId());
            row.put("personnelNumber", v.getPersonnel().getPersonnelNumber());
            row.put("rank", v.getPersonnel().getRank() != null ? v.getPersonnel().getRank() : "");
            row.put("shortName", v.getPersonnel().getShortName());
            row.put("startDate", v.getStartDate());
            row.put("endDate", v.getEndDate());
            row.put("vacationType", v.getVacationType());
            row.put("note", v.getNote());

            String status;
            if (v.getStartDate().isAfter(today))        status = "upcoming";
            else if (v.getEndDate().isBefore(today))    status = "past";
            else                                        status = "active";
            row.put("status", status);

            int days = (int) ChronoUnit.DAYS.between(v.getStartDate(), v.getEndDate()) + 1;
            row.put("days", days);
            if (v.getStartDate().isAfter(today)) {
                row.put("daysUntil", (int) ChronoUnit.DAYS.between(today, v.getStartDate()));
            }

            result.add(row);
        }

        // Активні → майбутні (ASC) → минулі (DESC)
        result.sort((a, b) -> {
            int pa = statusPriority((String) a.get("status"));
            int pb = statusPriority((String) b.get("status"));
            if (pa != pb) return pa - pb;
            LocalDate da = (LocalDate) a.get("startDate");
            LocalDate db = (LocalDate) b.get("startDate");
            if ("past".equals(a.get("status"))) return db.compareTo(da);
            return da.compareTo(db);
        });

        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("queueNum", i + 1);
        }
        return result;
    }

    private int statusPriority(String status) {
        switch (status) {
            case "active":   return 0;
            case "upcoming": return 1;
            case "past":     return 2;
            default:         return 3;
        }
    }

    @Transactional(readOnly = true)
    public List<Vacation> getPersonVacations(Long personnelId) {
        return vacationRepo.findByPersonnelId(personnelId);
    }

    @Transactional
    public Vacation create(Long personnelId, LocalDate start, LocalDate end,
                           String type, String note) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата початку не може бути пізніше дати кінця");
        }
        if (vacationRepo.existsOverlap(personnelId, start, end, null)) {
            throw new IllegalArgumentException("Період перетинається з існуючою відпусткою");
        }
        Personnel p = personnelRepo.findById(personnelId)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + personnelId));
        Vacation v = new Vacation();
        v.setPersonnel(p);
        v.setStartDate(start);
        v.setEndDate(end);
        v.setVacationType(type);
        v.setNote(note);
        Vacation saved = vacationRepo.save(v);
        log.info("Додано відпустку: особа={}, {} — {}, тип={}",
                p.getShortName(), start, end, type);
        return saved;
    }

    @Transactional
    public Vacation update(Long id, Long personnelId, LocalDate start, LocalDate end,
                           String type, String note) {
        Vacation v = vacationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Відпустку не знайдено: " + id));
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата початку не може бути пізніше дати кінця");
        }
        if (vacationRepo.existsOverlap(personnelId, start, end, id)) {
            throw new IllegalArgumentException("Період перетинається з існуючою відпусткою");
        }

        Personnel p = personnelRepo.findById(personnelId)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + personnelId));
        v.setPersonnel(p);
        v.setStartDate(start);
        v.setEndDate(end);
        v.setVacationType(type);
        v.setNote(note);
        return vacationRepo.save(v);
    }

    @Transactional
    public void delete(Long id) {
        vacationRepo.deleteById(id);
    }

    public List<Integer> getYears() {
        List<Integer> years = new ArrayList<>(vacationRepo.findDistinctYears());
        int currentYear = LocalDate.now().getYear();
        if (!years.contains(currentYear)) {
            years.add(currentYear);
        }
        Collections.sort(years);
        return years;
    }
}