package org.example.controller;

import org.example.entity.Personnel;
import org.example.service.PersonnelExportService;
import org.example.service.PersonnelService;
import org.example.service.PCardExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/personnel")
public class PersonnelController {

    private static final Logger log = LoggerFactory.getLogger(PersonnelController.class);
    private final PersonnelService service;
    private final PersonnelExportService exportService;
    private final PCardExportService pCardExportService;

    public PersonnelController(PersonnelService service,
                               PersonnelExportService exportService,
                               PCardExportService pCardExportService) {
        this.service = service;
        this.exportService = exportService;
        this.pCardExportService = pCardExportService;
    }

    // ===== СТОРІНКИ =====
    @GetMapping
    public String personnelPage() {
        return "personnel";
    }

    @GetMapping("/{id}/card")
    public String personCard(@PathVariable Long id, Model model) {
        Personnel p = service.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));
        model.addAttribute("person", p);
        return "person-card";
    }

    @GetMapping("/{id}/pcard")
    public String pCard(@PathVariable Long id, Model model) {
        Personnel p = service.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));
        model.addAttribute("person", p);
        return "pCard";
    }

    // ===== API =====
    @GetMapping("/api")
    @ResponseBody
    public List<Personnel> getAll() {
        return service.getAll();
    }

    @GetMapping("/api/archived")
    @ResponseBody
    public List<Personnel> getArchived() {
        return service.getArchived();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Personnel> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/status/{status}")
    @ResponseBody
    public List<Personnel> getByStatus(@PathVariable String status) {
        return service.getByStatus(status);
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<Personnel> search(@RequestParam String q) {
        return service.search(q);
    }

    // ===== CREATE (з обробкою IllegalArgumentException) =====
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody Personnel personnel) {
        try {
            if (personnel == null) {
                return ResponseEntity.badRequest().body("Тіло запиту порожнє");
            }
            if (personnel.getLastName() == null || personnel.getLastName().isBlank())
                return ResponseEntity.badRequest().body("Прізвище обов'язкове");
            if (personnel.getFirstName() == null || personnel.getFirstName().isBlank())
                return ResponseEntity.badRequest().body("Ім'я обов'язкове");

            // Дефолтний статус, якщо не задано
            if (personnel.getPersonnelStatus() == null || personnel.getPersonnelStatus().isEmpty()) {
                personnel.setPersonnelStatus("В особовому складі");
            }

            Personnel saved = service.create(personnel);
            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            // Перехоплюємо виняток про дублікат номера
            log.warn("Помилка при створенні: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (DataIntegrityViolationException e) {
            // Запасний варіант – якщо з якоїсь причини виняток не був перехоплений сервісом
            String message = e.getMessage();
            if (message != null && message.contains("personnel_number")) {
                return ResponseEntity.badRequest().body("Порядковий номер вже зайнятий");
            }
            log.error("Помилка цілісності даних", e);
            return ResponseEntity.badRequest().body("Помилка даних: " + e.getMessage());

        } catch (Exception e) {
            log.error("Помилка додавання особи", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    // ===== UPDATE (з обробкою IllegalArgumentException) =====
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Personnel personnel) {
        try {
            Personnel updated = service.update(id, personnel);
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            // Перехоплюємо виняток про дублікат номера або відсутність запису
            log.warn("Помилка при оновленні id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            log.error("Помилка оновлення особи id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    // ===== PATCH (часткове оновлення) – також додаємо обробку =====
    @PatchMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Personnel updated = service.patch(id, updates);
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            log.warn("Помилка при частковому оновленні id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            log.error("Помилка часткового оновлення id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    // ===== DELETE (деактивація) =====
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try {
            service.deactivate(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Помилка деактивації id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    // ===== ЕКСПОРТ =====
    @GetMapping("/api/{id}/export")
    public ResponseEntity<byte[]> exportPersonXlsx(@PathVariable Long id) {
        try {
            byte[] data = pCardExportService.exportPersonToXlsx(id);
            Personnel p = service.getById(id).orElse(null);
            String name = p != null ? p.getLastName() + "_" + p.getFirstName() : "Особа_" + id;
            String filename = URLEncoder.encode(name + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту особи id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String sortCol,
                                             @RequestParam(required = false) String sortDir) {
        try {
            List<Personnel> list;
            if (q != null && !q.isEmpty()) {
                list = service.search(q);
            } else {
                list = service.getAll();
            }

            // Сортування
            if (sortCol != null && !sortCol.isEmpty()) {
                boolean ascending = !"desc".equalsIgnoreCase(sortDir);
                Comparator<Personnel> comparator = switch (sortCol) {
                    case "num" -> Comparator.comparing(Personnel::getPersonnelNumber,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "rank" -> Comparator.comparing(Personnel::getRank,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "name" -> Comparator.comparing(Personnel::getLastName,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "position" -> Comparator.comparing(Personnel::getPosition,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "phone" -> Comparator.comparing(Personnel::getPhone,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "birth" -> Comparator.comparing(Personnel::getBirthDate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "age" -> Comparator.comparingInt(Personnel::getAge);
                    case "status" -> Comparator.comparing(Personnel::getPersonnelStatus,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    default -> Comparator.comparing(Personnel::getPersonnelNumber,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                };
                list.sort(ascending ? comparator : comparator.reversed());
            } else {
                list.sort(Comparator.comparing(Personnel::getPersonnelNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())));
            }

            byte[] data = exportService.exportToXlsx(list);
            String filename = URLEncoder.encode("Відомість_ОС.xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);

        } catch (Exception e) {
            log.error("Помилка експорту ОС", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}