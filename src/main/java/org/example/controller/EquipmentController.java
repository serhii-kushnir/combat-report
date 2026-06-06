package org.example.controller;

import org.example.entity.Equipment;
import org.example.service.EquipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public List<Equipment> getApi() {
        return service.getAll();
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> addItem(@RequestBody Equipment equipment) {
        try {
            if (equipment.getName() == null || equipment.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Назва не може бути порожньою");
            }
            Equipment saved = service.save(equipment);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Помилка: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateEquipment(@PathVariable Long id,
                                             @RequestBody Map<String, Object> updates) {
        try {
            Equipment eq = service.getById(id);
            if (updates.containsKey("name")) {
                String newName = (String) updates.get("name");
                if (newName == null || newName.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Назва не може бути порожньою");
                }
                eq.setName(newName);
            }
            if (updates.containsKey("quantity")) {
                eq.setQuantity(((Number) updates.get("quantity")).intValue());
            }
            if (updates.containsKey("unit")) {
                eq.setUnit((String) updates.get("unit"));
            }
            if (updates.containsKey("crew")) {
                eq.setCrew((String) updates.get("crew"));
            }
            if (updates.containsKey("location")) {
                eq.setLocation((String) updates.get("location"));
            }
            if (updates.containsKey("category")) {
                eq.setCategory((String) updates.get("category"));
            }
            service.save(eq);
            return ResponseEntity.ok().build();
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