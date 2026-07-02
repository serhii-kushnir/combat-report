package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.FlightRecord;
import org.example.repository.FlightRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public FlightRecordService(FlightRecordRepository repository) {
        this.repository = repository;
    }

    // ========== CRUD ==========

    public List<FlightRecord> getAll() {
        return repository.findAllByOrderByFlightDateAscRecordNumberAsc();
    }

    public List<FlightRecord> getByMonth(String month) {
        return repository.findByMonthOrderByRecordNumberAsc(month);
    }

    /** Унікальні роки з даних */
    public List<Integer> getYears() {
        return repository.findDistinctYears();
    }

    /** Записи за рік */
    public List<FlightRecord> getByYear(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to   = LocalDate.of(year, 12, 31);
        return repository.findByFlightDateBetweenOrderByFlightDateAscRecordNumberAsc(from, to);
    }

    // Порядок місяців для резервного сортування
    private static final List<String> MONTH_ORDER = List.of(
            "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
            "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
    );

    /**
     * Унікальні місяці відсортовані хронологічно.
     * Якщо записи місяця не мають дати — сортуємо по назві місяця.
     */
    public List<String> getMonths() {
        List<String> months = repository.findDistinctMonths();
        // Якщо БД повернула коректний порядок — повертаємо як є.
        // Якщо є місяці без дат — вони вже в кінці (NULLS LAST),
        // але їх між собою сортуємо по назві місяця.
        months.sort((a, b) -> {
            int ia = MONTH_ORDER.indexOf(a);
            int ib = MONTH_ORDER.indexOf(b);
            // Якщо обидва відомі місяці — сортуємо по порядку
            if (ia >= 0 && ib >= 0) return Integer.compare(ia, ib);
            // Невідомий місяць — в кінець
            if (ia < 0) return 1;
            if (ib < 0) return -1;
            return a.compareTo(b);
        });
        return months;
    }

    private int nextRecordNumber() {
        return repository.findMaxRecordNumber().map(n -> n + 1).orElse(1);
    }

    public FlightRecord save(FlightRecord r) {
        // Автонумерація — наступний порядковий номер якщо не вказано
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

    /**
     * Повертає наступний порядковий номер — максимальний існуючий + 1.
     * Якщо записів немає — повертає 1.
     */
    public int getNextRecordNumber() {
        return repository.findMaxRecordNumber().map(n -> n + 1).orElse(1);
    }

    // ========== ГЕНЕРАЦІЯ XLSX ==========

    public byte[] exportToXlsx(String monthFilter, Integer year) throws Exception {
        List<FlightRecord> records;
        if (monthFilter != null && !monthFilter.isBlank()) {
            records = getByMonth(monthFilter);
        } else if (year != null) {
            records = getByYear(year);
        } else {
            records = getAll();
        }

        // Групуємо по місяцях
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

            // Заголовки відповідають порядку
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

            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            for (Map.Entry<String, List<FlightRecord>> entry : byMonth.entrySet()) {
                XSSFSheet sheet = wb.createSheet(entry.getKey());

                for (int c = 0; c < COL_WIDTHS.length; c++) {
                    sheet.setColumnWidth(c, COL_WIDTHS[c] * 256);
                }

                // Заголовок
                XSSFRow hRow = sheet.createRow(0);
                hRow.setHeightInPoints(30);
                for (int c = 0; c < HEADERS.length; c++) {
                    XSSFCell cell = hRow.createCell(c);
                    cell.setCellValue(HEADERS[c]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (FlightRecord r : entry.getValue()) {
                    XSSFRow row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(40);

                    boolean isDestroyed = r.getEvent() != null &&
                            (r.getEvent().toLowerCase().contains("знищен") ||
                                    r.getEvent().toLowerCase().contains("підрив"));
                    XSSFCellStyle rowStyle = isDestroyed ? greenStyle : dataStyle;
                    XSSFCellStyle ctr = isDestroyed ? greenStyle : dataStyle;
                    XSSFCellStyle lft = isDestroyed ? greenLeftStyle : leftStyle;

                    int col = 0;

                    // 1. №
                    setCell(row, col++, r.getRecordNumber(), ctr);

                    // 2. Дата
                    String dateStr = r.getFlightDate() != null ? r.getFlightDate().format(dateFmt) : "";
                    setCell(row, col++, dateStr, ctr);

                    // 3. Екіпаж
                    setCell(row, col++, r.getCrew(), ctr);

                    // 4. Подія
                    setCell(row, col++, r.getEvent(), ctr);

                    // 5. Час взльоту
                    String takeoffStr = r.getTakeoffTime() != null ? r.getTakeoffTime().format(timeFmt) : "";
                    setCell(row, col++, takeoffStr, ctr);

                    // 6. Час втрати
                    String lossStr = r.getLossTime() != null ? r.getLossTime().format(timeFmt) : "";
                    setCell(row, col++, lossStr, ctr);

                    // 7. Координати
                    setCell(row, col++, r.getCoordinates(), ctr);

                    // 8. Азимут
                    String azimuthStr = r.getAzimuth() != null ? r.getAzimuth() + "°" : "";
                    setCell(row, col++, azimuthStr, ctr);

                    // 9. Відстань
                    setCell(row, col++, r.getDistance(), ctr);

                    // 10. Висота польоту
                    setCell(row, col++, r.getAltitude(), ctr);

                    // 11. Засіб ураження
                    setCell(row, col++, r.getWeapon(), lft);

                    // 12. Вибухівка
                    setCell(row, col++, r.getExplosive(), lft);

                    // 13. Детонатор
                    setCell(row, col++, r.getDetonator(), lft);

                    // 14. Висота цілі
                    setCell(row, col++, r.getTargetAltitude(), ctr);

                    // 15. Ціль
                    setCell(row, col++, r.getTarget(), ctr);

                    // 16. Швидкість цілі
                    setCell(row, col++, r.getTargetSpeed(), ctr);

                    // 17. Причина втрати
                    setCell(row, col++, r.getLossReason(), lft);

                    // 18. Примітка
                    setCell(row, col++, r.getNote(), lft);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ========== ХЕЛПЕРИ ДЛЯ ЗАПИСУ КЛІТИНОК ==========

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer i) cell.setCellValue(i);
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

    private XSSFCellStyle createDateStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle createGreenStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = createDataStyle(wb);
        // Світло-зелений фон як на веб-сторінці (#e8f5e9)
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