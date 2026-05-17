package org.example.controller;

import org.example.entity.Personnel;
import org.example.service.PersonnelService;
import org.example.service.PersonnelExportService;
import org.example.service.PersonnelImportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/personnel")
public class PersonnelController {

    private static final Logger log = LoggerFactory.getLogger(PersonnelController.class);
    private final PersonnelService service;
    private final PersonnelExportService exportService;
    private final PersonnelImportExportService importExportService;

    public PersonnelController(PersonnelService service, PersonnelExportService exportService, 
                               PersonnelImportExportService importExportService) {
        this.service = service;
        this.exportService = exportService;
        this.importExportService = importExportService;
    }

    @GetMapping
    public String personnelPage() {
        return "personnel";
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
            return ResponseEntity.ok(service.create(personnel));
        } catch (Exception e) {
            log.error("Помилка додавання особи: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
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
            log.error("Помилка оновлення особу id={}", id, e);
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

    @GetMapping("/api/export/full")
    public ResponseEntity<byte[]> exportFullVedomist() {
        try {
            byte[] data = importExportService.exportFullVedomist();
            String filename = URLEncoder.encode("Відомість_ОС_Повна.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту повної відомості", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/import")
    @ResponseBody
    public ResponseEntity<?> importVedomist(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Файл не обрано");
            }
            int count = importExportService.importFromVedomist(file);
            return ResponseEntity.ok("Імпортовано осіб: " + count);
        } catch (Exception e) {
            log.error("Помилка імпорту з Excel", e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }
}
