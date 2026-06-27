package org.example.controller;

import org.example.entity.Personnel;
import org.example.service.PersonnelExportService;
import org.example.service.PersonnelService;
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
import org.example.service.PCardExportService;

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

    public PersonnelController(PersonnelService service, PersonnelExportService exportService,
                               PCardExportService pCardExportService) {
        this.service = service;
        this.exportService = exportService;
        this.pCardExportService = pCardExportService;
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
            if (personnel.getLastName() == null || personnel.getLastName().isBlank())
                return ResponseEntity.badRequest().body("Прізвище обов'язкове");
            if (personnel.getFirstName() == null || personnel.getFirstName().isBlank())
                return ResponseEntity.badRequest().body("Ім'я обов'язкове");

            if (personnel.getPersonnelStatus() == null || personnel.getPersonnelStatus().isEmpty()) {
                personnel.setPersonnelStatus("В особовому складі");
            }

            Personnel saved = service.create(personnel);
            return ResponseEntity.ok(saved);

        } catch (DataIntegrityViolationException e) {
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

    @GetMapping("/api/{id}/export")
    public ResponseEntity<byte[]> exportPersonXlsx(@PathVariable Long id) {
        try {
            byte[] data = pCardExportService.exportPersonToXlsx(id);
            Personnel p = service.getById(id).orElse(null);
            String name = p != null ? p.getLastName() + "_" + p.getFirstName() : "Особа_" + id;
            String filename = URLEncoder.encode(name + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
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
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String sortCol,
                                             @RequestParam(required = false) String sortDir) {
        try {
            // Отримуємо список
            List<Personnel> list;
            if (q != null && !q.isEmpty()) {
                list = service.search(q);
            } else {
                list = service.getAll(); // або getAllPersonnel() – залежить від ваших потреб
            }

            // Сортуємо відповідно до параметрів
            if (sortCol != null && !sortCol.isEmpty()) {
                boolean ascending = !"desc".equalsIgnoreCase(sortDir);
                Comparator<Personnel> comparator = switch (sortCol) {
                    case "num" -> Comparator.comparing(Personnel::getPersonnelNumber,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    case "rank" -> Comparator.comparing(Personnel::getRank, Comparator.nullsLast(Comparator.naturalOrder()));
                    case "name" -> Comparator.comparing(Personnel::getLastName, Comparator.nullsLast(Comparator.naturalOrder()));
                    case "position" -> Comparator.comparing(Personnel::getPosition, Comparator.nullsLast(Comparator.naturalOrder()));
                    case "phone" -> Comparator.comparing(Personnel::getPhone, Comparator.nullsLast(Comparator.naturalOrder()));
                    case "birth" -> Comparator.comparing(Personnel::getBirthDate, Comparator.nullsLast(Comparator.naturalOrder()));
                    case "age" -> Comparator.comparingInt(Personnel::getAge);
                    case "status" -> Comparator.comparing(Personnel::getPersonnelStatus, Comparator.nullsLast(Comparator.naturalOrder()));
                    default -> Comparator.comparing(Personnel::getPersonnelNumber, Comparator.nullsLast(Comparator.naturalOrder()));
                };
                list.sort(ascending ? comparator : comparator.reversed());
            } else {
                // Сортування за замовчуванням – за personnelNumber
                list.sort(Comparator.comparing(Personnel::getPersonnelNumber, Comparator.nullsLast(Comparator.naturalOrder())));
            }

            byte[] data = exportService.exportToXlsx(list);
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