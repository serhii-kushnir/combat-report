package org.example.service;

import org.example.entity.Personnel;
import org.example.entity.Vlk;
import org.example.repository.PersonnelRepository;
import org.example.repository.VlkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class VlkService {

    private static final Logger log = LoggerFactory.getLogger(VlkService.class);

    private final VlkRepository vlkRepo;
    private final PersonnelRepository personnelRepo;

    public VlkService(VlkRepository vlkRepo, PersonnelRepository personnelRepo) {
        this.vlkRepo = vlkRepo;
        this.personnelRepo = personnelRepo;
    }

    // ============================================================
    //  МІСЯЧНА СІТКА (як у "Відпустках", але без півріч)
    // ============================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getYearData(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd   = LocalDate.of(year, 12, 31);

        List<Personnel> personnel = personnelRepo
                .findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        List<Vlk> allVlks = vlkRepo.findByYear(yearStart, yearEnd);

        Map<Long, List<Vlk>> byPerson = new HashMap<>();
        for (Vlk v : allVlks) {
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

            List<Vlk> personVlks = byPerson.getOrDefault(p.getId(), Collections.emptyList());

            Map<Integer, Integer> monthDays = new LinkedHashMap<>();
            for (int m = 1; m <= 12; m++) monthDays.put(m, 0);

            int totalDays = 0;
            for (Vlk v : personVlks) {
                LocalDate cursor = v.getStartDate().isBefore(yearStart) ? yearStart : v.getStartDate();
                LocalDate end    = v.getEndDate().isAfter(yearEnd) ? yearEnd : v.getEndDate();
                while (!cursor.isAfter(end)) {
                    monthDays.merge(cursor.getMonthValue(), 1, Integer::sum);
                    totalDays++;
                    cursor = cursor.plusDays(1);
                }
            }

            row.put("months", monthDays);
            row.put("totalDays", totalDays);
            row.put("vlks", personVlks);
            rows.add(row);
        }
        return rows;
    }

    // ============================================================
    //  ЧЕРГА "ХТО ЗА КИМ ІДЕ"
    // ============================================================
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQueue(Integer year) {
        List<Vlk> vlks;
        if (year != null) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd   = LocalDate.of(year, 12, 31);
            vlks = vlkRepo.findByYear(yearStart, yearEnd);
        } else {
            vlks = vlkRepo.findAllWithPersonnel();
        }

        LocalDate today = LocalDate.now();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Vlk v : vlks) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.getId());
            row.put("personnelId", v.getPersonnel().getId());
            row.put("personnelNumber", v.getPersonnel().getPersonnelNumber());
            row.put("rank", v.getPersonnel().getRank() != null ? v.getPersonnel().getRank() : "");
            row.put("shortName", v.getPersonnel().getShortName());
            row.put("startDate", v.getStartDate());
            row.put("endDate", v.getEndDate());
            row.put("vlkType", v.getVlkType());
            row.put("conclusion", v.getConclusion());
            row.put("note", v.getNote());

            String status;
            if (v.getStartDate().isAfter(today))       status = "upcoming";
            else if (v.getEndDate().isBefore(today))   status = "past";
            else                                       status = "active";
            row.put("status", status);

            int days = (int) ChronoUnit.DAYS.between(v.getStartDate(), v.getEndDate()) + 1;
            row.put("days", days);
            if (v.getStartDate().isAfter(today)) {
                row.put("daysUntil", (int) ChronoUnit.DAYS.between(today, v.getStartDate()));
            }

            result.add(row);
        }

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
    public List<Vlk> getPersonVlks(Long personnelId) {
        return vlkRepo.findByPersonnelId(personnelId);
    }

    @Transactional
    public Vlk create(Long personnelId, LocalDate start, LocalDate end,
                      String vlkType, String conclusion, String note) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата початку не може бути пізніше дати кінця");
        }
        if (vlkRepo.existsOverlap(personnelId, start, end, null)) {
            throw new IllegalArgumentException("Період перетинається з існуючим ВЛК");
        }
        Personnel p = personnelRepo.findById(personnelId)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + personnelId));
        Vlk v = new Vlk();
        v.setPersonnel(p);
        v.setStartDate(start);
        v.setEndDate(end);
        v.setVlkType(vlkType);
        v.setConclusion(conclusion);
        v.setNote(note);
        Vlk saved = vlkRepo.save(v);
        log.info("Додано ВЛК: особа={}, {} — {}, тип={}, заключення={}",
                p.getShortName(), start, end, vlkType, conclusion);
        return saved;
    }

    @Transactional
    public Vlk update(Long id, Long personnelId, LocalDate start, LocalDate end,
                      String vlkType, String conclusion, String note) {
        Vlk v = vlkRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ВЛК не знайдено: " + id));
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата початку не може бути пізніше дати кінця");
        }
        if (vlkRepo.existsOverlap(personnelId, start, end, id)) {
            throw new IllegalArgumentException("Період перетинається з існуючим ВЛК");
        }
        Personnel p = personnelRepo.findById(personnelId)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + personnelId));
        v.setPersonnel(p);
        v.setStartDate(start);
        v.setEndDate(end);
        v.setVlkType(vlkType);
        v.setConclusion(conclusion);
        v.setNote(note);
        return vlkRepo.save(v);
    }

    @Transactional
    public void delete(Long id) {
        vlkRepo.deleteById(id);
    }

    public List<Integer> getYears() {
        List<Integer> years = new ArrayList<>(vlkRepo.findDistinctYears());
        int currentYear = LocalDate.now().getYear();
        if (!years.contains(currentYear)) years.add(currentYear);
        Collections.sort(years);
        return years;
    }
}