package org.example.controller;

import org.example.entity.Personnel;
import org.example.service.PersonnelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/personnel")
public class PersonnelController {

    private static final Logger log = LoggerFactory.getLogger(PersonnelController.class);
    private final PersonnelService service;

    public PersonnelController(PersonnelService service) {
        this.service = service;
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
    public ResponseEntity<?> create(@RequestBody Personnel personnel) {
        try {
            if (personnel.getLastName() == null || personnel.getLastName().isBlank())
                return ResponseEntity.badRequest().body("Прізвище обов'язкове");
            if (personnel.getFirstName() == null || personnel.getFirstName().isBlank())
                return ResponseEntity.badRequest().body("Ім'я обов'язкове");
            return ResponseEntity.ok(service.create(personnel));
        } catch (Exception e) {
            log.error("Помилка додавання бійця", e);
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
            log.error("Помилка оновлення бійця id={}", id, e);
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PatchMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.updateStatus(id,
                    body.get("status"), body.getOrDefault("note", "")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
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
}
