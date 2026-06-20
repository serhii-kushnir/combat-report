package org.example.controller;

import org.example.entity.Personnel;
import org.example.service.PersonnelService;
import org.example.service.PersonnelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/personnel")
public class PersonnelController {

    private static final Logger log = LoggerFactory.getLogger(PersonnelController.class);
    private final PersonnelService service;
    private final PersonnelExportService exportService;

    public PersonnelController(PersonnelService service, PersonnelExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @PatchMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Personnel updated = service.patch(id, updates);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Помилка часткового оновлення id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

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

    @GetMapping("/api")
    @ResponseBody
    public List<Personnel> getAll() {
        return service.getAll();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Personnel> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pcard")
    public String pCard(@PathVariable Long id, Model model) {
        Personnel p = service.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));
        model.addAttribute("person", p);
        return "pCard";
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody(required = false) Personnel personnel) {
        try {
            if (personnel == null) {
                log.error("Тіло запиту порожнє або не вдалось десеріалізувати");
                return ResponseEntity.badRequest().body("Тіло запиту порожнє");
            }
            log.info("Отримано запит на додавання: lastName={}, firstName={}, birthDate={}, draftDate={}",
                    personnel.getLastName(), personnel.getFirstName(),
                    personnel.getBirthDate(), personnel.getDraftDate());
            if (personnel.getLastName() == null || personnel.getLastName().isBlank())
                return ResponseEntity.badRequest().body("Прізвище обов'язкове");
            if (personnel.getFirstName() == null || personnel.getFirstName().isBlank())
                return ResponseEntity.badRequest().body("Ім'я обов'язкове");

            Personnel saved = service.create(personnel);
            return ResponseEntity.ok(saved);

        } catch (DataIntegrityViolationException e) {
            // Перевіряємо, чи це помилка унікальності поля personnel_number
            // Оскільки personnel доступний, використовуємо його для логування
            String message = e.getMessage();
            if (message != null && message.contains("personnel_number")) {
                Integer duplicateNumber = personnel != null ? personnel.getPersonnelNumber() : null;
                log.warn("Спроба додати особу з вже існуючим порядковим номером: {}", duplicateNumber);
                return ResponseEntity.badRequest().body("Порядковий номер вже зайнятий");
            }
            log.error("Помилка цілісності даних при додаванні особи: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Помилка даних: " + e.getMessage());

        } catch (Exception e) {
            log.error("Помилка додавання особи: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/api/status/{status}")
    @ResponseBody
    public List<Personnel> getByStatus(@PathVariable String status) {
        return service.getByStatus(status);
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Personnel personnel) {
        try {
            return ResponseEntity.ok(service.update(id, personnel));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Помилка оновлення особи id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try {
            service.deactivate(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<Personnel> search(@RequestParam String q) {
        return service.search(q);
    }

    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx() {
        try {
            byte[] data = exportService.exportToXlsx();
            String filename = URLEncoder.encode("Відомість_ОС.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
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