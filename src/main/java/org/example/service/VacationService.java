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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getYearData(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<Personnel> personnel = personnelRepo
                .findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        List<Vacation> allVacations = vacationRepo.findByYear(yearStart, yearEnd);

        // Групуємо за id особи
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

            // Підрахунок днів по місяцях
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
            row.put("hasH1", h1Days > 0);
            row.put("hasH2", h2Days > 0);
            row.put("vacations", personVacations);
            rows.add(row);
        }
        return rows;
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

        // ===== ЗАВЖДИ завантажуємо особу заново (навіть якщо не змінилась) =====
        Personnel p = personnelRepo.findById(personnelId)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + personnelId));
        v.setPersonnel(p);
        // ======================================================================

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