package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.FlightRecord;
import org.example.repository.FlightRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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

    // ========== ІМПОРТ XLSX ==========

    @Transactional
    public int importFromXlsx(MultipartFile file) throws Exception {
        log.info("Імпорт файлу: {}", file.getOriginalFilename());
        int imported = 0;

        // Назви місяців для відповідності листів
        Map<String, Integer> MONTH_ORDER = new LinkedHashMap<>();
        MONTH_ORDER.put("Січень", 1); MONTH_ORDER.put("Лютий", 2);
        MONTH_ORDER.put("Березень", 3); MONTH_ORDER.put("Квітень", 4);
        MONTH_ORDER.put("Травень", 5); MONTH_ORDER.put("Червень", 6);
        MONTH_ORDER.put("Липень", 7); MONTH_ORDER.put("Серпень", 8);
        MONTH_ORDER.put("Вересень", 9); MONTH_ORDER.put("Жовтень", 10);
        MONTH_ORDER.put("Листопад", 11); MONTH_ORDER.put("Грудень", 12);

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            for (Sheet sheet : wb) {
                String sheetName = sheet.getSheetName();
                if (!MONTH_ORDER.containsKey(sheetName)) {
                    log.debug("Пропускаємо лист: {}", sheetName);
                    continue;
                }

                log.info("Обробляємо лист: {}", sheetName);
                int firstDataRow = 1; // рядок 0 — заголовок

                for (int i = firstDataRow; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    // Перевіряємо що є порядковий номер
                    Integer num = getInt(row.getCell(0));
                    if (num == null) continue;

                    // Пропускаємо якщо вже є в БД
                    if (repository.existsByRecordNumber(num)) {
                        log.debug("Запис №{} вже існує, пропускаємо", num);
                        continue;
                    }

                    FlightRecord r = new FlightRecord();
                    r.setRecordNumber(num);
                    r.setFlightDate(getDate(row.getCell(1)));
                    r.setCrew(getString(row.getCell(2)));
                    r.setEvent(getString(row.getCell(3)));
                    r.setTakeoffTime(getTime(row.getCell(4)));
                    r.setLossTime(getTime(row.getCell(5)));
                    r.setCoordinates(getString(row.getCell(6)));
                    r.setDistance(getInt(row.getCell(7)));
                    r.setTargetType(getString(row.getCell(8)));
                    r.setIdentification(getString(row.getCell(9)));
                    r.setWeapon(getString(row.getCell(10)));
                    r.setExplosive(getString(row.getCell(11)));
                    r.setDetonator(getString(row.getCell(12)));
                    r.setAltitude(getString(row.getCell(13)));
                    r.setTarget(getString(row.getCell(14)));
                    r.setTargetSpeed(getInt(row.getCell(15)));
                    r.setNote(getString(row.getCell(16)));
                    r.setMonth(sheetName);

                    repository.save(r);
                    imported++;
                }
            }
        }

        log.info("Імпортовано {} записів", imported);
        return imported;
    }

    // ========== ГЕНЕРАЦІЯ XLSX ==========

    public byte[] exportToXlsx(String monthFilter) throws Exception {
        List<FlightRecord> records = monthFilter == null || monthFilter.isBlank()
                ? getAll()
                : getByMonth(monthFilter);

        // Групуємо по місяцях
        Map<String, List<FlightRecord>> byMonth = new LinkedHashMap<>();
        for (FlightRecord r : records) {
            byMonth.computeIfAbsent(r.getMonth(), k -> new ArrayList<>()).add(r);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Стилі
            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle   = createDataStyle(wb);
            XSSFCellStyle dateStyle   = createDateStyle(wb);

            String[] HEADERS = {"№", "Дата", "Екіпаж", "Подія", "Час взльоту",
                    "Час втрати", "Координати", "Відстань (м)", "Тип",
                    "Ідентифікація", "Засіб ураження", "Вибухівка", "Детонатор",
                    "Висота (м)", "Ціль", "Швидкість цілі (км/год)", "Примітка"};
            int[] COL_WIDTHS = {8, 14, 12, 24, 14, 14, 20, 14, 14,
                    14, 22, 26, 28, 12, 20, 22, 50};

            for (Map.Entry<String, List<FlightRecord>> entry : byMonth.entrySet()) {
                XSSFSheet sheet = wb.createSheet(entry.getKey());

                // Ширина колонок
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

                // Дані
                int rowIdx = 1;
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                for (FlightRecord r : entry.getValue()) {
                    XSSFRow row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(40);

                    setCell(row, 0, r.getRecordNumber(), dataStyle);
                    setCell(row, 1, r.getFlightDate() != null
                            ? r.getFlightDate().format(dateFmt) : "", dataStyle);
                    setCell(row, 2, r.getCrew(), dataStyle);
                    setCell(row, 3, r.getEvent(), dataStyle);
                    setCell(row, 4, r.getTakeoffTime() != null
                            ? r.getTakeoffTime().format(timeFmt) : "", dataStyle);
                    setCell(row, 5, r.getLossTime() != null
                            ? r.getLossTime().format(timeFmt) : "", dataStyle);
                    setCell(row, 6, r.getCoordinates(), dataStyle);
                    setCell(row, 7, r.getDistance(), dataStyle);
                    setCell(row, 8, r.getTargetType(), dataStyle);
                    setCell(row, 9, r.getIdentification(), dataStyle);
                    setCell(row, 10, r.getWeapon(), dataStyle);
                    setCell(row, 11, r.getExplosive(), dataStyle);
                    setCell(row, 12, r.getDetonator(), dataStyle);
                    setCell(row, 13, r.getAltitude(), dataStyle);
                    setCell(row, 14, r.getTarget(), dataStyle);
                    setCell(row, 15, r.getTargetSpeed(), dataStyle);
                    setCell(row, 16, r.getNote(), dataStyle);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ========== ХЕЛПЕРИ ДЛЯ ЧИТАННЯ КЛІТИНОК ==========

    private String getString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK   -> null;
            default      -> null;
        };
    }

    private Integer getInt(Cell cell) {
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (int) cell.getNumericCellValue();
                case STRING  -> Integer.parseInt(cell.getStringCellValue().trim());
                default      -> null;
            };
        } catch (Exception e) { return null; }
    }

    private LocalDate getDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();
                // Формати: dd.MM.yyyy або yyyy-MM-dd
                if (s.matches("\\d{2}\\.\\d{2}\\.\\d{4}"))
                    return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                if (s.matches("\\d{4}-\\d{2}-\\d{2}"))
                    return LocalDate.parse(s);
            }
        } catch (Exception e) { log.warn("Не вдалось розпарсити дату: {}", cell); }
        return null;
    }

    private LocalTime getTime(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                // Excel зберігає час як дробову частину дня
                double val = cell.getNumericCellValue();
                int totalSeconds = (int) Math.round(val * 86400);
                return LocalTime.ofSecondOfDay(totalSeconds % 86400);
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();
                if (s.matches("\\d{1,2}:\\d{2}")) return LocalTime.parse(s.length() == 4 ? "0" + s : s);
                if (s.matches("\\d{1,2}:\\d{2}:\\d{2}")) return LocalTime.parse(s.length() == 7 ? "0" + s : s);
            }
        } catch (Exception e) { log.warn("Не вдалось розпарсити час: {}", cell); }
        return null;
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
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createDateStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void setBorders(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}