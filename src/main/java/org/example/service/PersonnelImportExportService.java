package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.Personnel;
import org.example.repository.PersonnelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class PersonnelImportExportService {

    private static final Logger log = LoggerFactory.getLogger(PersonnelImportExportService.class);
    private static final DateTimeFormatter DATE_FMT_UA = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PersonnelRepository repository;

    public PersonnelImportExportService(PersonnelRepository repository) {
        this.repository = repository;
    }

    /**
     * Експорт у форматі "Відомість ОС" з 50 стовпцями
     */
    public byte[] exportFullVedomist() throws Exception {
        List<Personnel> list = repository.findByActiveTrueOrderByLastNameAsc();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Відомість ОС");
            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle = createDataStyle(wb);
            XSSFCellStyle centerStyle = createCenterStyle(wb);
            XSSFCellStyle dateStyle = createDateStyle(wb);

            // Заголовки згідно з файлом Відомость_ОС.xlsx (50 стовпців)
            String[] HEADERS = {
                "№", "Прізвище", "Ім'я", "По батькові", "Військове звання", "Посада", "Повна посада",
                "Телефон", "Дата народження", "ІПН", "Пас. серія", "Пас. номер", "Група крові",
                "Адреса реєстрації", "Адреса проживання", "Сімейний стан", "Дружина/Чоловік",
                "Кількість дітей", "Дата призову", "Вид служби", "Ким призваний", "УБД №",
                "Водійське посвідчення", "Рівень освіти", "Заклад освіти", "Спеціальність",
                "Форма навчання", "Дата вступу", "Дата закінчення", "Диплом №",
                "Дата зарахування", "Дата звільнення", "Форма допуску", "Номер допуску",
                "Попередня в/ч", "Бойовий досвід", "Наявність нагород", "Перелік нагород",
                "Група інвалідності", "Причина інвалідності", "Вид мобілізації",
                "Початок контракту", "Кінець контракту", "Зарплатна картка",
                "Розмір форми", "Розмір взуття", "Контактна особа", "Додаткова інформація", "Примітка"
            };

            int[] WIDTHS = {
                6, 18, 14, 18, 20, 20, 30,
                14, 14, 14, 10, 12, 10,
                30, 30, 14, 25,
                8, 14, 14, 25, 12,
                14, 20, 35, 25,
                14, 14, 14, 16,
                14, 14, 16, 12,
                20, 25, 14, 30,
                14, 25, 14,
                14, 14, 20,
                12, 12, 30, 40, 30
            };

            // Заголовок
            XSSFRow hRow = sheet.createRow(0);
            hRow.setHeightInPoints(35);
            for (int c = 0; c < HEADERS.length; c++) {
                sheet.setColumnWidth(c, WIDTHS[c] * 256);
                XSSFCell cell = hRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            // Дані
            for (int i = 0; i < list.size(); i++) {
                Personnel p = list.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.setHeightInPoints(22);

                int col = 0;
                setCell(row, col++, i + 1, centerStyle);
                setCell(row, col++, p.getLastName(), dataStyle);
                setCell(row, col++, p.getFirstName(), dataStyle);
                setCell(row, col++, p.getMiddleName(), dataStyle);
                setCell(row, col++, p.getRank(), dataStyle);
                setCell(row, col++, p.getPosition(), dataStyle);
                setCell(row, col++, p.getFullPosition(), dataStyle);
                setCell(row, col++, p.getPhone(), centerStyle);
                setCell(row, col++, fmt(p.getBirthDate()), centerStyle);
                setCell(row, col++, p.getTaxId(), centerStyle);
                setCell(row, col++, p.getPassportSeries(), centerStyle);
                setCell(row, col++, p.getPassportNumber(), centerStyle);
                setCell(row, col++, p.getBloodGroup(), centerStyle);
                setCell(row, col++, p.getRegistrationAddress(), dataStyle);
                setCell(row, col++, p.getLivingAddress(), dataStyle);
                setCell(row, col++, p.getMaritalStatus(), centerStyle);
                setCell(row, col++, p.getSpouseName(), dataStyle);
                setCell(row, col++, "", centerStyle); // Кількість дітей (з окремої таблиці)
                setCell(row, col++, fmt(p.getDraftDate()), centerStyle);
                setCell(row, col++, p.getServiceType(), centerStyle);
                setCell(row, col++, p.getDraftOrganization(), dataStyle);
                setCell(row, col++, p.getUbdNumber(), centerStyle);
                setCell(row, col++, p.getDriverLicense(), centerStyle);
                setCell(row, col++, p.getEducationLevel(), dataStyle);
                setCell(row, col++, p.getEducationInstitution(), dataStyle);
                setCell(row, col++, p.getEducationSpeciality(), dataStyle);
                setCell(row, col++, p.getEducationForm(), centerStyle);
                setCell(row, col++, fmt(p.getEducationStart()), centerStyle);
                setCell(row, col++, fmt(p.getEducationEnd()), centerStyle);
                setCell(row, col++, p.getDiplomaNumber(), centerStyle);
                setCell(row, col++, p.getAdmissionDate(), centerStyle);
                setCell(row, col++, p.getDismissalDate(), centerStyle);
                setCell(row, col++, p.getAccessForm(), centerStyle);
                setCell(row, col++, p.getAccessNumber(), centerStyle);
                setCell(row, col++, p.getMilitaryUnitPrevious(), dataStyle);
                setCell(row, col++, p.getCombatExperience(), dataStyle);
                setCell(row, col++, p.getAwardStatus(), centerStyle);
                setCell(row, col++, p.getAwardsList(), dataStyle);
                setCell(row, col++, p.getDisabilityGroup(), centerStyle);
                setCell(row, col++, p.getDisabilityCause(), dataStyle);
                setCell(row, col++, p.getMobilizationType(), centerStyle);
                setCell(row, col++, p.getContractStartDate(), centerStyle);
                setCell(row, col++, p.getContractEndDate(), centerStyle);
                setCell(row, col++, p.getSalaryCard(), dataStyle);
                setCell(row, col++, p.getUniformSize(), centerStyle);
                setCell(row, col++, p.getShoeSize(), centerStyle);
                setCell(row, col++, p.getEmergencyContact(), dataStyle);
                setCell(row, col++, p.getAdditionalInfo(), dataStyle);
                setCell(row, col++, p.getNote(), dataStyle);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Імпорт з файлу "Відомість ОС"
     */
    public int importFromVedomist(MultipartFile file) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = wb.getSheetAt(0);
            
            int importedCount = 0;
            int skippedCount = 0;
            
            // Пропускаємо заголовок (рядок 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Personnel p = parseRow(row);
                    if (p != null && p.getLastName() != null && !p.getLastName().isBlank()) {
                        repository.save(p);
                        importedCount++;
                        log.info("Імпортовано: {} {} {}", p.getLastName(), p.getFirstName(), p.getMiddleName());
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.error("Помилка імпорту рядка {}: {}", i, e.getMessage());
                    skippedCount++;
                }
            }
            
            log.info("Імпорт завершено: додано={}, пропущено={}", importedCount, skippedCount);
            return importedCount;
        }
    }

    private Personnel parseRow(Row row) {
        Personnel p = new Personnel();
        
        p.setLastName(getCellString(row, 1));
        p.setFirstName(getCellString(row, 2));
        p.setMiddleName(getCellString(row, 3));
        p.setRank(getCellString(row, 4));
        p.setPosition(getCellString(row, 5));
        p.setFullPosition(getCellString(row, 6));
        p.setPhone(getCellString(row, 7));
        p.setBirthDate(parseDate(getCellString(row, 8)));
        p.setTaxId(getCellString(row, 9));
        p.setPassportSeries(getCellString(row, 10));
        p.setPassportNumber(getCellString(row, 11));
        p.setBloodGroup(getCellString(row, 12));
        p.setRegistrationAddress(getCellString(row, 13));
        p.setLivingAddress(getCellString(row, 14));
        p.setMaritalStatus(getCellString(row, 15));
        p.setSpouseName(getCellString(row, 16));
        p.setDraftDate(parseDate(getCellString(row, 18)));
        p.setServiceType(getCellString(row, 19));
        p.setDraftOrganization(getCellString(row, 20));
        p.setUbdNumber(getCellString(row, 21));
        p.setDriverLicense(getCellString(row, 22));
        p.setEducationLevel(getCellString(row, 23));
        p.setEducationInstitution(getCellString(row, 24));
        p.setEducationSpeciality(getCellString(row, 25));
        p.setEducationForm(getCellString(row, 26));
        p.setEducationStart(parseDate(getCellString(row, 27)));
        p.setEducationEnd(parseDate(getCellString(row, 28)));
        p.setDiplomaNumber(getCellString(row, 29));
        p.setAdmissionDate(getCellString(row, 30));
        p.setDismissalDate(getCellString(row, 31));
        p.setAccessForm(getCellString(row, 32));
        p.setAccessNumber(getCellString(row, 33));
        p.setMilitaryUnitPrevious(getCellString(row, 34));
        p.setCombatExperience(getCellString(row, 35));
        p.setAwardStatus(getCellString(row, 36));
        p.setAwardsList(getCellString(row, 37));
        p.setDisabilityGroup(getCellString(row, 38));
        p.setDisabilityCause(getCellString(row, 39));
        p.setMobilizationType(getCellString(row, 40));
        p.setContractStartDate(getCellString(row, 41));
        p.setContractEndDate(getCellString(row, 42));
        p.setSalaryCard(getCellString(row, 43));
        p.setUniformSize(getCellString(row, 44));
        p.setShoeSize(getCellString(row, 45));
        p.setEmergencyContact(getCellString(row, 46));
        p.setAdditionalInfo(getCellString(row, 47));
        p.setNote(getCellString(row, 48));
        p.setActive(true);
        
        return p;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getLocalDateTimeCellValue().toLocalDate();
                    return d.format(DATE_FMT_UA);
                }
                double val = cell.getNumericCellValue();
                if (val == (long) val) return String.valueOf((long) val);
                return String.valueOf(val);
            case BOOLEAN: return cell.getBooleanCellValue() ? "Так" : "Ні";
            default: return "";
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        dateStr = dateStr.trim();
        try {
            return LocalDate.parse(dateStr, DATE_FMT_UA);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(dateStr, DATE_FMT_ISO);
            } catch (DateTimeParseException e2) {
                log.warn("Не вдалось розпарсити дату: {}", dateStr);
                return null;
            }
        }
    }

    private String fmt(LocalDate d) {
        return d != null ? d.format(DATE_FMT_UA) : "";
    }

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer i) cell.setCellValue(i);
        else cell.setCellValue(value.toString());
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26,(byte)35,(byte)126}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        f.setBold(true); f.setFontHeightInPoints((short)11); f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s); return s;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short)10); f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s); return s;
    }

    private XSSFCellStyle createCenterStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = createDataStyle(wb);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle createDateStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = createCenterStyle(wb);
        CreationHelper ch = wb.getCreationHelper();
        s.setDataFormat(ch.createDataFormat().getFormat("dd.mm.yyyy"));
        return s;
    }

    private void setBorders(XSSFCellStyle s) {
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);  s.setBorderRight(BorderStyle.THIN);
    }
}
