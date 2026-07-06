package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.FlightRecord;
import org.example.repository.FlightRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FlightRecordService {

    private static final Logger log = LoggerFactory.getLogger(FlightRecordService.class);

    private final FlightRecordRepository repository;
    private static final List<String> MONTH_ORDER = List.of(
            "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
            "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
    );

    public FlightRecordService(FlightRecordRepository repository) {
        this.repository = repository;
    }

    // ======================================================================
    //  БЕЗ ПАГІНАЦІЇ (збережено для експорту та сумісності)
    // ======================================================================

    public List<FlightRecord> getAll() {
        return repository.findAllByOrderByFlightDateAscRecordNumberAsc();
    }

    public List<FlightRecord> getByMonth(String month) {
        return repository.findByMonthOrderByRecordNumberAsc(month);
    }

    public List<FlightRecord> getByYear(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to   = LocalDate.of(year, 12, 31);
        return repository.findByFlightDateBetweenOrderByFlightDateAscRecordNumberAsc(from, to);
    }

    public List<Integer> getYears() {
        return repository.findDistinctYears();
    }

    public List<String> getMonths() {
        List<String> months = repository.findDistinctMonths();
        months.sort((a, b) -> {
            int ia = MONTH_ORDER.indexOf(a);
            int ib = MONTH_ORDER.indexOf(b);
            if (ia >= 0 && ib >= 0) return Integer.compare(ia, ib);
            if (ia < 0) return 1;
            if (ib < 0) return -1;
            return a.compareTo(b);
        });
        return months;
    }

    @Transactional
    public FlightRecord save(FlightRecord r) {
        if (r.getRecordNumber() == null) {
            r.setRecordNumber(nextRecordNumber());
        }
        return repository.save(r);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<FlightRecord> getById(Long id) {
        return repository.findById(id);
    }

    public int getNextRecordNumber() {
        return repository.findMaxRecordNumber().map(n -> n + 1).orElse(1);
    }

    private int nextRecordNumber() {
        return repository.findMaxRecordNumber().map(n -> n + 1).orElse(1);
    }

    // ======================================================================
    //  МЕТОДИ З ПАГІНАЦІЄЮ
    // ======================================================================

    public Page<FlightRecord> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("flightDate").ascending().and(Sort.by("recordNumber").ascending()));
        return repository.findAll(pageable);
    }

    public Page<FlightRecord> getByMonth(String month, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("recordNumber").ascending());
        return repository.findByMonth(month, pageable);
    }

    public Page<FlightRecord> getByYear(int year, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("flightDate").ascending().and(Sort.by("recordNumber").ascending()));
        return repository.findByYear(year, pageable);
    }

    // ======================================================================
    //  ЕКСПОРТ В XLSX (без змін, використовує getByMonth / getByYear без пагінації)
    // ======================================================================

    public byte[] exportToXlsx(String monthFilter, Integer year) throws Exception {
        List<FlightRecord> records;
        if (monthFilter != null && !monthFilter.isBlank()) {
            records = getByMonth(monthFilter);
        } else if (year != null) {
            records = getByYear(year);
        } else {
            records = getAll();
        }

        Map<String, List<FlightRecord>> byMonth = new LinkedHashMap<>();
        for (FlightRecord r : records) {
            byMonth.computeIfAbsent(r.getMonth(), k -> new ArrayList<>()).add(r);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle   = createDataStyle(wb);
            XSSFCellStyle greenStyle  = createGreenStyle(wb);
            XSSFCellStyle leftStyle   = createLeftStyle(wb);
            XSSFCellStyle greenLeftStyle = createGreenStyle(wb);
            greenLeftStyle.setAlignment(HorizontalAlignment.LEFT);

            String[] HEADERS = {
                    "№", "Дата", "Екіпаж", "Подія", "Час взльоту", "Час втрати",
                    "Координати", "Азимут (°)", "Відстань (м)", "Вис. польоту (м)",
                    "Засіб ураження", "Вибухівка", "Детонатор",
                    "Висота цілі (м)", "Ціль", "Швидкість цілі (км/год)",
                    "Причина втрати", "Примітка"
            };
            int[] COL_WIDTHS = {
                    8, 14, 12, 24, 12, 12,
                    22, 10, 14, 14,
                    22, 26, 28,
                    14, 20, 20,
                    22, 50
            };

            for (Map.Entry<String, List<FlightRecord>> entry : byMonth.entrySet()) {
                XSSFSheet sheet = wb.createSheet(entry.getKey());
                for (int c = 0; c < COL_WIDTHS.length; c++) {
                    sheet.setColumnWidth(c, COL_WIDTHS[c] * 256);
                }
                XSSFRow hRow = sheet.createRow(0);
                hRow.setHeightInPoints(30);
                for (int c = 0; c < HEADERS.length; c++) {
                    XSSFCell cell = hRow.createCell(c);
                    cell.setCellValue(HEADERS[c]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                for (FlightRecord r : entry.getValue()) {
                    XSSFRow row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(40);

                    boolean isDestroyed = r.getEvent() != null &&
                            (r.getEvent().toLowerCase().contains("знищен") ||
                                    r.getEvent().toLowerCase().contains("підрив"));
                    XSSFCellStyle ctr = isDestroyed ? greenStyle : dataStyle;
                    XSSFCellStyle lft = isDestroyed ? greenLeftStyle : leftStyle;

                    int col = 0;
                    setCell(row, col++, r.getRecordNumber(), ctr);
                    setCell(row, col++, r.getFlightDate() != null ? r.getFlightDate().format(dateFmt) : "", ctr);
                    setCell(row, col++, r.getCrew(), ctr);
                    setCell(row, col++, r.getEvent(), ctr);
                    setCell(row, col++, r.getTakeoffTime() != null ? r.getTakeoffTime().format(timeFmt) : "", ctr);
                    setCell(row, col++, r.getLossTime() != null ? r.getLossTime().format(timeFmt) : "", ctr);
                    setCell(row, col++, r.getCoordinates(), ctr);
                    setCell(row, col++, r.getAzimuth() != null ? r.getAzimuth() + "°" : "", ctr);
                    setCell(row, col++, r.getDistance(), ctr);
                    setCell(row, col++, r.getAltitude(), ctr);
                    setCell(row, col++, r.getWeapon(), lft);
                    setCell(row, col++, r.getExplosive(), lft);
                    setCell(row, col++, r.getDetonator(), lft);
                    setCell(row, col++, r.getTargetAltitude(), ctr);
                    setCell(row, col++, r.getTarget(), ctr);
                    setCell(row, col++, r.getTargetSpeed(), ctr);
                    setCell(row, col++, r.getLossReason(), lft);
                    setCell(row, col++, r.getNote(), lft);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ХЕЛПЕРИ ДЛЯ СТИЛІВ =====

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer) cell.setCellValue((Integer) value);
        else cell.setCellValue(value.toString());
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26, (byte)35, (byte)126}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createLeftStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private XSSFCellStyle createGreenStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = createDataStyle(wb);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)232, (byte)245, (byte)233}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setBorders(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}