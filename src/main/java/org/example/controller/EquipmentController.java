package org.example.controller;

import org.example.entity.Equipment;
import org.example.entity.EquipmentHistory;
import org.example.service.EquipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/equipment")
public class EquipmentController {

    private static final Logger log = LoggerFactory.getLogger(EquipmentController.class);
    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", service.getAll());
        return "equipment";
    }

    @GetMapping("/api")
    @ResponseBody
    public Page<Equipment> getApi(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return service.getPage(page, size);
    }

    // ===== ІСТОРІЯ =====
    @GetMapping("/api/{id}/history")
    @ResponseBody
    public ResponseEntity<List<EquipmentHistory>> getHistory(@PathVariable Long id) {
        try {
            List<EquipmentHistory> history = service.getHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Помилка отримання історії для equipment id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> addItem(@RequestBody Equipment equipment,
                                     Principal principal) {
        try {
            if (equipment.getName() == null || equipment.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Назва не може бути порожньою");
            }
            String changedBy = principal != null ? principal.getName() : "system";
            Equipment saved = service.save(equipment, changedBy);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateEquipment(@PathVariable Long id,
                                             @RequestBody Map<String, Object> updates,
                                             Principal principal) {
        try {
            String changedBy = principal != null ? principal.getName() : "system";
            Equipment updated = service.updateFields(id, updates, changedBy);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @GetMapping("/api/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportToXlsx() {
        try {
            byte[] data = service.exportToXlsx();
            String filename = URLEncoder.encode("Майно.xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту майна", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}