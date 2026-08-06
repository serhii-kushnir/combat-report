package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.Equipment;
import org.example.entity.EquipmentHistory;
import org.example.repository.EquipmentHistoryRepository;
import org.example.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class);

    private final EquipmentRepository repository;
    private final EquipmentHistoryRepository historyRepository;

    public EquipmentService(EquipmentRepository repository,
                            EquipmentHistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    public List<Equipment> getAll() {
        return repository.findAll();
    }

    public Equipment save(Equipment equipment, String changedBy) {
        if (changedBy != null) {
            equipment.setModifiedBy(changedBy);
        }
        equipment.setLastModified(LocalDateTime.now());
        return repository.save(equipment);
    }

    public void delete(Long id) {
        historyRepository.deleteAll(historyRepository.findByEquipmentIdOrderByChangedAtDesc(id));
        repository.deleteById(id);
    }

    public Equipment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Не знайдено запис з id=" + id));
    }

    public Page<Equipment> getPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return repository.findAll(pageable);
    }

    public List<EquipmentHistory> getHistory(Long equipmentId) {
        return historyRepository.findByEquipmentIdOrderByChangedAtDesc(equipmentId);
    }

    @Transactional
    public Equipment updateFields(Long id, Map<String, Object> updates, String changedBy) {
        Equipment eq = getById(id);
        if (changedBy == null) changedBy = "system";

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "name":
                    String newName = (String) value;
                    if (newName != null && !newName.trim().isEmpty() && !newName.equals(eq.getName())) {
                        saveHistory(eq.getId(), "name", eq.getName(), newName, changedBy);
                        eq.setName(newName);
                    }
                    break;
                case "quantity":
                    int newQty = ((Number) value).intValue();
                    if (newQty != eq.getQuantity()) {
                        saveHistory(eq.getId(), "quantity", String.valueOf(eq.getQuantity()), String.valueOf(newQty), changedBy);
                        eq.setQuantity(newQty);
                    }
                    break;
                case "stockQuantity":
                    int newStock = ((Number) value).intValue();
                    if (newStock != eq.getStockQuantity()) {
                        saveHistory(eq.getId(), "stockQuantity", String.valueOf(eq.getStockQuantity()), String.valueOf(newStock), changedBy);
                        eq.setStockQuantity(newStock);
                    }
                    break;
                case "writtenOffQuantity":
                    int newWrittenOff = ((Number) value).intValue();
                    if (newWrittenOff != eq.getWrittenOffQuantity()) {
                        saveHistory(eq.getId(), "writtenOffQuantity", String.valueOf(eq.getWrittenOffQuantity()), String.valueOf(newWrittenOff), changedBy);
                        eq.setWrittenOffQuantity(newWrittenOff);
                    }
                    break;
                case "unit":
                    String newUnit = (String) value;
                    if (newUnit != null && !newUnit.equals(eq.getUnit())) {
                        saveHistory(eq.getId(), "unit", eq.getUnit(), newUnit, changedBy);
                        eq.setUnit(newUnit);
                    }
                    break;
                case "crew":
                    String newCrew = (String) value;
                    if (newCrew != null && !newCrew.equals(eq.getCrew())) {
                        saveHistory(eq.getId(), "crew", eq.getCrew(), newCrew, changedBy);
                        eq.setCrew(newCrew);
                    }
                    break;
                case "location":
                    String newLocation = (String) value;
                    if (newLocation != null && !newLocation.equals(eq.getLocation())) {
                        saveHistory(eq.getId(), "location", eq.getLocation(), newLocation, changedBy);
                        eq.setLocation(newLocation);
                    }
                    break;
                case "category":
                    String newCategory = (String) value;
                    if (newCategory != null && !newCategory.equals(eq.getCategory())) {
                        saveHistory(eq.getId(), "category", eq.getCategory(), newCategory, changedBy);
                        eq.setCategory(newCategory);
                    }
                    break;
            }
        }

        eq.setLastModified(LocalDateTime.now());
        eq.setModifiedBy(changedBy);
        return repository.save(eq);
    }

    private void saveHistory(Long equipmentId, String fieldName, String oldValue, String newValue, String changedBy) {
        EquipmentHistory history = new EquipmentHistory();
        history.setEquipmentId(equipmentId);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    // ===== ЕКСПОРТ XLSX =====
    public byte[] exportToXlsx() throws Exception {
        List<Equipment> items = getAll();
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Майно");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);

            String[] headers = {
                    "№", "Назва",
                    "Кількість на позиції",
                    "Кількість на складі",
                    "Кількість списано",
                    "Одиниць.", "Екіпаж", "Локація", "Категорія",
                    "Остання зміна", "Ким змінено"
            };
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, (headers[i].length() + 6) * 256);
            }

            int rowNum = 1;
            for (Equipment eq : items) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(18);
                setCell(row, 0, rowNum - 1, dataStyle);
                setCell(row, 1, eq.getName(), dataStyle);
                setCell(row, 2, eq.getQuantity() != null ? eq.getQuantity() : 0, dataStyle);
                setCell(row, 3, eq.getStockQuantity() != null ? eq.getStockQuantity() : 0, dataStyle);
                setCell(row, 4, eq.getWrittenOffQuantity() != null ? eq.getWrittenOffQuantity() : 0, dataStyle);
                setCell(row, 5, eq.getUnit(), dataStyle);
                setCell(row, 6, eq.getCrew(), dataStyle);
                setCell(row, 7, eq.getLocation(), dataStyle);
                setCell(row, 8, eq.getCategory(), dataStyle);
                setCell(row, 9, eq.getLastModified() != null ? eq.getLastModified().toString().replace('T', ' ') : "", dataStyle);
                setCell(row, 10, eq.getModifiedBy(), dataStyle);
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

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}