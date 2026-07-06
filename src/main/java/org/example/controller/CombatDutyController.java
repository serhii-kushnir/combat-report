package org.example.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.CombatDuty;
import org.example.service.CombatDutyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/combat-duty")
public class CombatDutyController {

    private static final Logger log = LoggerFactory.getLogger(CombatDutyController.class);
    private final CombatDutyService service;

    public CombatDutyController(CombatDutyService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        return "combat-duty";
    }

    // ===== API З ПАГІНАЦІЄЮ =====
    @GetMapping("/api")
    @ResponseBody
    public Page<CombatDuty> getAllApi(@RequestParam(required = false) Integer year,
                                      @RequestParam(required = false) Integer month,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        if (year != null && month != null) {
            return service.getByYearAndMonth(year, month, page, size);
        } else if (year != null) {
            return service.getByYear(year, page, size);
        } else {
            return service.getPage(page, size);
        }
    }

    // ===== ЗАГАЛЬНА СТАТИСТИКА =====
    @GetMapping("/api/general-stats")
    @ResponseBody
    public Map<String, Long> getGeneralStats() {
        return service.getGeneralStats();
    }

    // ===== ДЛЯ ФІЛЬТРІВ (роки, місяці) =====
    @GetMapping("/api/years")
    @ResponseBody
    public List<Integer> getYears() {
        return service.getAll().stream()
                .map(d -> d.getStartTime().toLocalDate().getYear())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @GetMapping("/api/months")
    @ResponseBody
    public List<String> getMonths() {
        String[] monthNames = {"Січень","Лютий","Березень","Квітень","Травень","Червень",
                "Липень","Серпень","Вересень","Жовтень","Листопад","Грудень"};
        return service.getAll().stream()
                .map(d -> monthNames[d.getStartTime().toLocalDate().getMonthValue() - 1])
                .distinct()
                .sorted((a, b) -> {
                    for (int i = 0; i < monthNames.length; i++) {
                        if (monthNames[i].equals(a)) return Integer.compare(i, Arrays.asList(monthNames).indexOf(b));
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<CombatDuty> getOne(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== CRUD =====
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody CombatDuty duty) {
        try {
            if (duty.getStartTime() == null || duty.getEndTime() == null) {
                return ResponseEntity.badRequest().body("Необхідно вказати час початку та кінця");
            }
            if (duty.getStartTime().isAfter(duty.getEndTime())) {
                return ResponseEntity.badRequest().body("Дата початку не може бути пізніше дати кінця");
            }
            if (service.hasOverlap(duty, null)) {
                return ResponseEntity.badRequest().body("Цей період уже зайнятий іншим чергуванням");
            }
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
            if (duty.getStartTime() == null || duty.getEndTime() == null) {
                return ResponseEntity.badRequest().body("Необхідно вказати час початку та кінця");
            }
            if (duty.getStartTime().isAfter(duty.getEndTime())) {
                return ResponseEntity.badRequest().body("Дата початку не може бути пізніше дати кінця");
            }
            if (service.hasOverlap(duty, id)) {
                return ResponseEntity.badRequest().body("Цей період уже зайнятий іншим чергуванням");
            }
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

    // ===== ЕКСПОРТ XLSX (оновлений – розбивка по місяцях) =====
    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String search) {
        try {
            List<CombatDuty> allDuties = service.getAll();

            // Фільтр за роком/місяцем
            if (year != null || month != null) {
                allDuties = allDuties.stream()
                        .filter(d -> {
                            LocalDate date = d.getStartTime().toLocalDate();
                            boolean match = true;
                            if (year != null) match = match && date.getYear() == year;
                            if (month != null) match = match && date.getMonthValue() == month;
                            return match;
                        })
                        .collect(Collectors.toList());
            }

            // Фільтр за пошуком (як у фронтенді)
            if (search != null && !search.isBlank()) {
                String lowerSearch = search.toLowerCase().trim();
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                allDuties = allDuties.stream()
                        .filter(d -> {
                            String idStr = String.valueOf(d.getId());
                            String startDate = d.getStartTime() != null ? d.getStartTime().format(dateFmt) : "";
                            String endDate = d.getEndTime() != null ? d.getEndTime().format(dateFmt) : "";
                            String startTime = d.getStartTime() != null ? d.getStartTime().format(timeFmt) : "";
                            String endTime = d.getEndTime() != null ? d.getEndTime().format(timeFmt) : "";
                            boolean textMatch = Stream.of(
                                    d.getUnitName(), d.getCommander(), d.getPilot(), d.getNavigator(),
                                    d.getTechnician(), d.getWeapons(), d.getDutyOfficer(), d.getReportSummary(),
                                    startDate, endDate, startTime, endTime
                            ).anyMatch(f -> f != null && f.toLowerCase().contains(lowerSearch));
                            return idStr.contains(lowerSearch) || textMatch;
                        })
                        .collect(Collectors.toList());
            }

            // Групування по місяцях (рік-місяць)
            Map<String, List<CombatDuty>> dutiesByMonth = allDuties.stream()
                    .collect(Collectors.groupingBy(d -> {
                        LocalDate date = d.getStartTime().toLocalDate();
                        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                    }));

            if (dutiesByMonth.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            byte[] data = exportToXlsx(dutiesByMonth);
            String filename = URLEncoder.encode(
                    "Бойове_чергування" +
                            (year != null ? "_" + year : "") +
                            (month != null ? "_" + month : "") +
                            (search != null && !search.isBlank() ? "_search" : "") +
                            ".xlsx",
                    StandardCharsets.UTF_8
            ).replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            log.error("Помилка експорту", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== ПОБУДОВА XLSX (окремі аркуші по місяцях) =====
    private byte[] exportToXlsx(Map<String, List<CombatDuty>> dutiesByMonth) throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String[] monthNames = {"Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"};

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<String> sortedKeys = new ArrayList<>(dutiesByMonth.keySet());
            Collections.sort(sortedKeys);

            for (String key : sortedKeys) {
                List<CombatDuty> list = dutiesByMonth.get(key);
                String[] parts = key.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                String sheetName = monthNames[month - 1] + " " + year;
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                Sheet sheet = wb.createSheet(sheetName);
                buildSheet(sheet, list, fmt);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ПОБУДОВА ОДНОГО АРКУША =====
    private void buildSheet(Sheet sheet, List<CombatDuty> list, DateTimeFormatter fmt) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle centerStyle = createCenterStyle(sheet.getWorkbook());
        CellStyle leftStyle = createLeftStyle(sheet.getWorkbook()); // з перенесенням

        String[] headers = {
                "ID", "Початок", "Кінець", "Екіпаж", "Командир", "Пілот", "Штурман", "Технік",
                "Озброєння", "Черговий ПУ", "Доповідь", "Вильотів", "Бойових", "Втрат", "Знищень", "НТП"
        };
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, (headers[i].length() + 6) * 256);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (CombatDuty d : list) {
            Row row = sheet.createRow(rowNum++);
            row.setHeight((short)-1);  // автоматична висота

            int col = 0;
            createCell(row, col++, d.getId(), centerStyle);
            createCell(row, col++, d.getStartTime() != null ? d.getStartTime().format(fmt) : "", centerStyle);
            createCell(row, col++, d.getEndTime() != null ? d.getEndTime().format(fmt) : "", centerStyle);
            createCell(row, col++, d.getUnitName(), centerStyle);
            createCell(row, col++, d.getCommander(), leftStyle);
            createCell(row, col++, d.getPilot(), leftStyle);
            createCell(row, col++, d.getNavigator(), leftStyle);
            createCell(row, col++, d.getTechnician(), leftStyle);
            createCell(row, col++, d.getWeapons(), centerStyle);
            createCell(row, col++, d.getDutyOfficer(), centerStyle);
            createCell(row, col++, d.getReportSummary(), leftStyle);  // текст з перенесенням
            createCell(row, col++, d.getTotalSorties() != null ? d.getTotalSorties() : 0, centerStyle);
            createCell(row, col++, d.getCombatSorties() != null ? d.getCombatSorties() : 0, centerStyle);
            createCell(row, col++, d.getLosses() != null ? d.getLosses() : 0, centerStyle);
            createCell(row, col++, d.getDestructions() != null ? d.getDestructions() : 0, centerStyle);
            createCell(row, col++, d.getNtp() != null ? d.getNtp() : 0, centerStyle);
        }

        // Автоширина – для колонки "Доповідь" (індекс 10) вже встановлено 80*256
        for (int i = 0; i < headers.length; i++) {
            if (i == 10) {
                sheet.setColumnWidth(i, 80 * 256);
            } else {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 512);
            }
        }
    }

    // ===== ДОПОМІЖНІ МЕТОДИ ДЛЯ КОМІРОК =====
    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createLeftStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);  // ← ДОДАНО: перенесення тексту
        setBorders(s);
        return s;
    }

    private void setBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}