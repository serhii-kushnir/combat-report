package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.Personnel;
import org.example.entity.PersonnelChild;
import org.example.entity.PersonnelEducation;
import org.example.entity.PersonnelWeapon;
import org.example.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PersonnelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PersonnelService personnelService;
    private final PersonnelEducationRepository eduRepo;
    private final PersonnelChildRepository childRepo;
    private final PersonnelWeaponRepository weaponRepo;

    // Конструктор – тільки три репозиторії
    public PersonnelExportService(PersonnelService personnelService,
                                  PersonnelEducationRepository eduRepo,
                                  PersonnelChildRepository childRepo,
                                  PersonnelWeaponRepository weaponRepo) {
        this.personnelService = personnelService;
        this.eduRepo = eduRepo;
        this.childRepo = childRepo;
        this.weaponRepo = weaponRepo;
    }

    // ===== ЕКСПОРТ ВСІХ ОСІБ (ВІДОМІСТЬ) =====
    public byte[] exportToXlsx() throws Exception {
        List<Personnel> list = personnelService.getAll();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Відомість ОС");

            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle = createDataStyle(wb);
            XSSFCellStyle centerStyle = createCenterStyle(wb);

            String[] HEADERS = {
                    "№", "Прізвище", "Ім'я", "По батькові", "Звання", "Коротка посада", "Повна посада", "Телефон",
                    "Дата народження", "Повних років", "Ідентифікаційний код", "Паспорт (серія, номер)", "Група крові",
                    "Водійське посвідчення (серія, номер, категорія)", "Здобута освіта", "Заклад освіти", "Спеціальність",
                    "Дата початку", "Дата завершення", "Адреса місця реєстрації", "Адреса проживання",
                    "Сімейний стан", "Дружина/Чоловік", "Кількість дітей", "Дата народження (дитини)", "Повних років (дитини)",
                    "Адреса проживання (сім'я)", "Ким призваний", "Дата призову", "Вид служби", "УБД №",
                    "Форма допуску", "Зарахування", "Військова служба за", "Зброя (модель, №, дата)", "Примітка"
            };
            int[] WIDTHS = {
                    6, 18, 14, 18, 15, 20, 30, 14,
                    14, 10, 14, 16, 10,
                    20, 18, 30, 25,
                    14, 14, 30, 30,
                    16, 20, 10, 14, 12,
                    30, 20, 14, 14, 14,
                    20, 20, 14, 25, 30
            };

            XSSFRow hRow = sheet.createRow(0);
            hRow.setHeightInPoints(30);
            for (int c = 0; c < HEADERS.length; c++) {
                sheet.setColumnWidth(c, WIDTHS[c] * 256);
                XSSFCell cell = hRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < list.size(); i++) {
                Personnel p = list.get(i);
                XSSFRow row = sheet.createRow(rowNum++);
                row.setHeightInPoints(20);

                List<PersonnelEducation> edus = eduRepo.findByPersonnelIdOrderByStartDateAsc(p.getId());
                PersonnelEducation firstEdu = edus.isEmpty() ? null : edus.get(0);
                List<PersonnelChild> children = childRepo.findByPersonnelIdOrderByBirthDateAsc(p.getId());
                PersonnelChild firstChild = children.isEmpty() ? null : children.get(0);
                int childCount = children.size();
                List<PersonnelWeapon> weapons = weaponRepo.findByPersonnelId(p.getId());
                String weaponInfo = "";
                if (!weapons.isEmpty()) {
                    PersonnelWeapon w = weapons.get(0);
                    weaponInfo = Stream.of(w.getWeaponType(), w.getSerialNumber(), w.getIssuedDate())
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "));
                }

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
                setCell(row, col++, p.getBirthDate() != null ? String.valueOf(calcAge(p.getBirthDate())) : "", centerStyle);
                setCell(row, col++, p.getTaxId(), centerStyle);
                String passport = Stream.of(p.getPassportSeries(), p.getPassportNumber())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" "));
                setCell(row, col++, passport, centerStyle);
                setCell(row, col++, p.getBloodGroup(), centerStyle);
                String driver = Stream.of(p.getDriverLicenseSeries(), p.getDriverLicenseNumber(), p.getDriverLicenseCategory())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" "));
                setCell(row, col++, driver, centerStyle);
                setCell(row, col++, firstEdu != null ? firstEdu.getLevel() : "", dataStyle);
                setCell(row, col++, firstEdu != null ? firstEdu.getInstitution() : "", dataStyle);
                setCell(row, col++, firstEdu != null ? firstEdu.getSpeciality() : "", dataStyle);
                setCell(row, col++, firstEdu != null ? fmt(firstEdu.getStartDate()) : "", centerStyle);
                setCell(row, col++, firstEdu != null ? fmt(firstEdu.getEndDate()) : "", centerStyle);
                setCell(row, col++, p.getRegistrationAddress(), dataStyle);
                setCell(row, col++, p.getLivingAddress(), dataStyle);
                setCell(row, col++, p.getMaritalStatus(), centerStyle);
                setCell(row, col++, p.getSpouseName(), dataStyle);
                setCell(row, col++, childCount == 0 ? "" : String.valueOf(childCount), centerStyle);
                setCell(row, col++, firstChild != null ? fmt(firstChild.getBirthDate()) : "", centerStyle);
                int childAge = (firstChild != null && firstChild.getBirthDate() != null) ? calcAge(firstChild.getBirthDate()) : 0;
                setCell(row, col++, childAge == 0 ? "" : String.valueOf(childAge), centerStyle);
                setCell(row, col++, p.getFamilyAddress(), dataStyle);
                setCell(row, col++, p.getDraftOrganization(), dataStyle);
                setCell(row, col++, fmt(p.getDraftDate()), centerStyle);
                setCell(row, col++, p.getServiceType(), centerStyle);
                setCell(row, col++, p.getUbdNumber(), centerStyle);
                setCell(row, col++, p.getAdmissionForm(), dataStyle);
                setCell(row, col++, p.getEnrollmentInfo(), dataStyle);
                setCell(row, col++, p.getServiceFor(), centerStyle);
                setCell(row, col++, weaponInfo, dataStyle);
                setCell(row, col++, p.getNote(), dataStyle);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==================== ДОПОМІЖНІ МЕТОДИ ====================

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer) cell.setCellValue((Integer) value);
        else if (value instanceof Double) cell.setCellValue((Double) value);
        else if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value.toString());
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26, (byte)35, (byte)126}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s);
        return s;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Arial");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorders(s);
        return s;
    }

    private XSSFCellStyle createCenterStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = createDataStyle(wb);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private void setBorders(XSSFCellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private String fmt(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "";
    }

    private int calcAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        LocalDate now = LocalDate.now();
        int age = now.getYear() - birthDate.getYear();
        if (now.getMonthValue() < birthDate.getMonthValue() ||
                (now.getMonthValue() == birthDate.getMonthValue() && now.getDayOfMonth() < birthDate.getDayOfMonth())) {
            age--;
        }
        return age;
    }
}