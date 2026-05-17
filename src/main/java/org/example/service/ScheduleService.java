package org.example.service;

import org.example.entity.Personnel;
import org.example.entity.ScheduleEntry;
import org.example.repository.PersonnelRepository;
import org.example.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepo;
    private final PersonnelRepository personnelRepo;

    public ScheduleService(ScheduleRepository scheduleRepo,
                           PersonnelRepository personnelRepo) {
        this.scheduleRepo = scheduleRepo;
        this.personnelRepo = personnelRepo;
    }

    /**
     * Повертає список рядків для рендерингу таблиці.
     * Кожен рядок: { id, shortName, rank, days: { 1: "БЧ", 2: "", ... } }
     */
    public List<Map<String, Object>> getMonthData(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();
        int daysInMonth = ym.lengthOfMonth();

        // Всі активні бійці
        List<Personnel> allPersonnel = personnelRepo.findByActiveTrueOrderByLastNameAsc();

        // Всі записи за місяць
        List<ScheduleEntry> entries = scheduleRepo.findByMonth(from, to);

        // Індексуємо: personnelId → { day → status }
        Map<Long, Map<Integer, String>> index = new HashMap<>();
        for (ScheduleEntry e : entries) {
            Long pid = e.getPersonnel().getId();
            index.computeIfAbsent(pid, k -> new HashMap<>())
                 .put(e.getDate().getDayOfMonth(), e.getStatus());
        }

        // Формуємо результат
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Personnel p : allPersonnel) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("shortName", p.getShortName());
            row.put("rank", p.getRank() != null ? p.getRank() : "");

            Map<Integer, String> days = new LinkedHashMap<>();
            Map<Integer, String> personDays = index.getOrDefault(p.getId(), Collections.emptyMap());
            for (int d = 1; d <= daysInMonth; d++) {
                days.put(d, personDays.getOrDefault(d, ""));
            }
            row.put("days", days);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Встановити або очистити статус бійця на дату.
     * status == "" або null → видалити запис.
     */
    @Transactional
    public void setStatus(Long personnelId, LocalDate date, String status) {
        Optional<ScheduleEntry> existing =
                scheduleRepo.findByPersonnelIdAndDate(personnelId, date);

        if (status == null || status.isBlank()) {
            existing.ifPresent(e -> {
                scheduleRepo.delete(e);
                log.info("Видалено запис: боєць={}, дата={}", personnelId, date);
            });
            return;
        }

        if (existing.isPresent()) {
            existing.get().setStatus(status);
            scheduleRepo.save(existing.get());
        } else {
            Personnel p = personnelRepo.findById(personnelId)
                    .orElseThrow(() -> new IllegalArgumentException("Боєць не знайдений: " + personnelId));
            scheduleRepo.save(new ScheduleEntry(p, date, status));
        }
        log.info("Графік: боєць={}, дата={}, статус={}", personnelId, date, status);
    }

    /**
     * Підрахунок кожного статусу за місяць для кожного бійця.
     * personnelId → { статус → кількість }
     */
    public Map<Long, Map<String, Long>> getMonthlyCounts(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<ScheduleEntry> entries = scheduleRepo.findByMonth(ym.atDay(1), ym.atEndOfMonth());

        Map<Long, Map<String, Long>> result = new HashMap<>();
        for (ScheduleEntry e : entries) {
            Long pid = e.getPersonnel().getId();
            result.computeIfAbsent(pid, k -> new HashMap<>())
                  .merge(e.getStatus(), 1L, Long::sum);
        }
        return result;
    }
}
