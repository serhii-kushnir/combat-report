package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entity.Equipment;
import org.example.repository.EquipmentRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository repository;

    public EquipmentService(EquipmentRepository repository) {
        this.repository = repository;
    }

    public List<Equipment> getAll() {
        return repository.findAll();
    }

    public Equipment save(Equipment equipment) {
        return repository.save(equipment);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Equipment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Не знайдено запис з id=" + id));
    }

    // ========== ЕКСПОРТ В XLSX ==========

    public byte[] exportToXlsx() throws Exception {
        List<Equipment> items = getAll();
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Майно");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);

            String[] headers = {"№", "Назва", "Кількість", "Одиниць.", "Екіпаж", "Локація", "Категорія"};
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

                setCell(row, 0, rowNum - 1, dataStyle); // №
                setCell(row, 1, eq.getName(), dataStyle);
                setCell(row, 2, eq.getQuantity(), dataStyle);
                setCell(row, 3, eq.getUnit(), dataStyle);
                setCell(row, 4, eq.getCrew(), dataStyle);
                setCell(row, 5, eq.getLocation(), dataStyle);
                setCell(row, 6, eq.getCategory(), dataStyle);
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