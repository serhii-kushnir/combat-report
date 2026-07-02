package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.CombatDuty;
import org.example.service.CombatDutyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/combat-duty")
public class CombatDutyController {

    private final CombatDutyService service;

    public CombatDutyController(CombatDutyService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("duties", service.getAll());
        return "combat-duty";
    }

    // ===== API =====

    @GetMapping("/api")
    @ResponseBody
    public List<CombatDuty> getAllApi(@RequestParam(required = false) String q) {
        List<CombatDuty> all = service.getAll();
        if (q != null && !q.isEmpty()) {
            String filter = q.toLowerCase();
            return all.stream()
                    .filter(d ->
                            (d.getCommander() != null && d.getCommander().toLowerCase().contains(filter)) ||
                            (d.getPilot() != null && d.getPilot().toLowerCase().contains(filter)) ||
                            (d.getNavigator() != null && d.getNavigator().toLowerCase().contains(filter)) ||
                            (d.getTechnician() != null && d.getTechnician().toLowerCase().contains(filter)) ||
                            (d.getUnitName() != null && d.getUnitName().toLowerCase().contains(filter)) ||
                            (d.getWeapons() != null && d.getWeapons().toLowerCase().contains(filter)) ||
                            (d.getDutyOfficer() != null && d.getDutyOfficer().toLowerCase().contains(filter)) ||
                            (d.getReportSummary() != null && d.getReportSummary().toLowerCase().contains(filter))
                    )
                    .collect(Collectors.toList());
        }
        return all;
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<CombatDuty> getOneApi(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody CombatDuty duty) {
        try {
            CombatDuty saved = service.save(duty);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CombatDuty duty) {
        try {
            duty.setId(id);
            CombatDuty updated = service.save(duty);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Помилка: " + e.getMessage());
        }
    }

    // ===== ЕКСПОРТ XLSX =====

    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String q) {
        try {
            List<CombatDuty> list = getAllApi(q);
            byte[] data = exportToXlsx(list);
            String filename = URLEncoder.encode("Бойове_чергування.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] exportToXlsx(List<CombatDuty> list) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Чергування");

            // Висота рядків за замовчуванням (30 pt)
            sheet.setDefaultRowHeightInPoints(30);

            // Стиль заголовка
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Стиль для даних (центр)
            CellStyle centerDataStyle = wb.createCellStyle();
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerDataStyle.setWrapText(true);

            // Стиль для даних (лівий край)
            CellStyle leftDataStyle = wb.createCellStyle();
            leftDataStyle.setAlignment(HorizontalAlignment.LEFT);
            leftDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            leftDataStyle.setWrapText(true);

            // Заголовки та ширини колонок
            String[] headers = {
                    "ID", "Початок", "Кінець", "Екіпаж", "Командир", "Пілот", "Штурман", "Технік",
                    "Озброєння", "Черговий ПУ", "Доповідь", "Вильотів", "Бойових", "Втрат", "Знищень", "НТП"
            };
            int[] widths = {
                    8, 16, 16, 12, 25, 22, 22, 22, 18, 20, 80, 10, 10, 10, 10, 10
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (CombatDuty d : list) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(30);
                int col = 0;

                setCell(row, col++, d.getId(), centerDataStyle);
                setCell(row, col++, d.getStartTime() != null ? d.getStartTime().format(fmt) : "", centerDataStyle);
                setCell(row, col++, d.getEndTime() != null ? d.getEndTime().format(fmt) : "", centerDataStyle);
                setCell(row, col++, d.getUnitName(), centerDataStyle);
                setCell(row, col++, d.getCommander(), leftDataStyle);
                setCell(row, col++, d.getPilot(), leftDataStyle);
                setCell(row, col++, d.getNavigator(), leftDataStyle);
                setCell(row, col++, d.getTechnician(), leftDataStyle);
                setCell(row, col++, d.getWeapons(), centerDataStyle);
                setCell(row, col++, d.getDutyOfficer(), centerDataStyle);
                setCell(row, col++, d.getReportSummary(), leftDataStyle);
                setCell(row, col++, d.getTotalSorties() != null ? d.getTotalSorties() : 0, centerDataStyle);
                setCell(row, col++, d.getCombatSorties() != null ? d.getCombatSorties() : 0, centerDataStyle);
                setCell(row, col++, d.getLosses() != null ? d.getLosses() : 0, centerDataStyle);
                setCell(row, col++, d.getDestructions() != null ? d.getDestructions() : 0, centerDataStyle);
                setCell(row, col++, d.getNtp() != null ? d.getNtp() : 0, centerDataStyle);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }



}