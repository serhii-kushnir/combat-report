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

    public PersonnelExportService(PersonnelService personnelService,
                                  PersonnelEducationRepository eduRepo,
                                  PersonnelChildRepository childRepo,
                                  PersonnelWeaponRepository weaponRepo) {
        this.personnelService = personnelService;
        this.eduRepo = eduRepo;
        this.childRepo = childRepo;
        this.weaponRepo = weaponRepo;
    }

    public byte[] exportToXlsx() throws Exception {
        List<Personnel> list = personnelService.getAll();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ===== АРКУШ 1: Розширена відомість (48 колонок) =====
            XSSFSheet sheet = wb.createSheet("Відомість ОС");

            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle = createDataStyle(wb);
            XSSFCellStyle centerStyle = createCenterStyle(wb);

            String[] HEADERS = {
                    "№", "Статус", "Звання", "ПІБ", "Телефон", "Дата народж.",
                    "Вік", "ІПН", "Група крові", "Паспорт серія", "Паспорт №",
                    "Вод. серія", "Вод. №", "Вод. категорія",
                    "Адреса реєстрації", "Адреса проживання",
                    "Сімейний стан", "Дружина/Чоловік", "Адреса сім'ї",
                    "Освіта рівень", "Заклад освіти", "Спеціальність",
                    "Початок", "Кінець", "Диплом №",
                    "Посада кор.", "Посада повна", "Військова частина",
                    "ВОС", "Тариф", "Дата призову", "Ким призваний",
                    "Область призову", "Місто/Сел. призову",
                    "Дата зарахув.", "Наказ зарахув.",
                    "УБД №", "УБД дата",
                    "Форма допуску", "Ф-Наказ", "Ф-Дата",
                    "Служба за", "Розм. взуття", "Розм. форми", "Розм. гол. убору",
                    "Зброя (модель, №, дата)", "Примітка"
            };

            int[] WIDTHS = {
                    6, 14, 16, 30, 16, 16, 6, 16, 14, 16, 16,
                    16, 16, 16,
                    30, 30,
                    16, 24, 30,
                    16, 30, 24,
                    14, 14, 16,
                    20, 30, 18,
                    12, 12, 14, 24,
                    18, 18,
                    16, 18,
                    16, 14,
                    16, 16, 16,
                    16, 12, 12, 14,
                    30, 30
            };

            XSSFRow headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(30);
            for (int c = 0; c < HEADERS.length; c++) {
                sheet.setColumnWidth(c, WIDTHS[c] * 256);
                XSSFCell cell = headerRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < list.size(); i++) {
                Personnel p = list.get(i);
                XSSFRow row = sheet.createRow(rowNum++);
                row.setHeightInPoints(20);

                // Отримуємо першу освіту, дітей та зброю
                List<PersonnelEducation> edus = eduRepo.findByPersonnelIdOrderByStartDateAsc(p.getId());
                PersonnelEducation firstEdu = edus.isEmpty() ? null : edus.get(0);
                List<PersonnelChild> children = childRepo.findByPersonnelIdOrderByBirthDateAsc(p.getId());
                PersonnelChild firstChild = children.isEmpty() ? null : children.get(0);
                List<PersonnelWeapon> weapons = weaponRepo.findByPersonnelId(p.getId());
                String weaponInfo = "";
                if (!weapons.isEmpty()) {
                    PersonnelWeapon w = weapons.get(0);
                    weaponInfo = Stream.of(w.getWeaponType(), w.getSerialNumber(), w.getIssuedDate())
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "));
                }

                int col = 0;
                setCell(row, col++, i + 1, centerStyle);                                     // №
                setCell(row, col++, p.getPersonnelStatus(), centerStyle);                    // Статус
                setCell(row, col++, p.getRank(), centerStyle);                               // Звання
                setCell(row, col++, p.getFullName(), dataStyle);                             // ПІБ
                setCell(row, col++, p.getPhone(), centerStyle);                              // Телефон
                setCell(row, col++, fmt(p.getBirthDate()), centerStyle);                     // Дата народж.
                setCell(row, col++, p.getBirthDate() != null ? String.valueOf(calcAge(p.getBirthDate())) : "", centerStyle); // Вік
                setCell(row, col++, p.getTaxId(), centerStyle);                              // ІПН
                setCell(row, col++, p.getBloodGroup(), centerStyle);                         // Група крові
                setCell(row, col++, p.getPassportSeries(), centerStyle);                     // Паспорт серія
                setCell(row, col++, p.getPassportNumber(), centerStyle);                     // Паспорт №
                setCell(row, col++, p.getDriverLicenseSeries(), centerStyle);                // Вод. серія
                setCell(row, col++, p.getDriverLicenseNumber(), centerStyle);                // Вод. №
                setCell(row, col++, p.getDriverLicenseCategory(), centerStyle);              // Вод. категорія
                setCell(row, col++, p.getRegistrationAddress(), dataStyle);                  // Адреса реєстрації
                setCell(row, col++, p.getLivingAddress(), dataStyle);                        // Адреса проживання
                setCell(row, col++, p.getMaritalStatus(), centerStyle);                      // Сімейний стан
                setCell(row, col++, p.getSpouseName(), dataStyle);                           // Дружина/Чоловік
                setCell(row, col++, p.getFamilyAddress(), dataStyle);                        // Адреса сім'ї

                // Освіта (перший запис)
                setCell(row, col++, firstEdu != null ? firstEdu.getLevel() : "", dataStyle);
                setCell(row, col++, firstEdu != null ? firstEdu.getInstitution() : "", dataStyle);
                setCell(row, col++, firstEdu != null ? firstEdu.getSpeciality() : "", dataStyle);
                setCell(row, col++, firstEdu != null ? fmt(firstEdu.getStartDate()) : "", centerStyle);
                setCell(row, col++, firstEdu != null ? fmt(firstEdu.getEndDate()) : "", centerStyle);
                setCell(row, col++, firstEdu != null ? firstEdu.getDiploma() : "", centerStyle);

                setCell(row, col++, p.getPosition(), dataStyle);                              // Посада кор.
                setCell(row, col++, p.getFullPosition(), dataStyle);                          // Посада повна
                setCell(row, col++, p.getMilitaryUnit(), dataStyle);                          // Військова частина
                setCell(row, col++, p.getVos(), centerStyle);                                 // ВОС
                setCell(row, col++, p.getTariffGrade(), centerStyle);                         // Тариф
                setCell(row, col++, fmt(p.getDraftDate()), centerStyle);                      // Дата призову
                setCell(row, col++, p.getDraftOrganization(), dataStyle);                     // Ким призваний
                setCell(row, col++, p.getDrafObl(), dataStyle);                               // Область призову
                setCell(row, col++, p.getDraftLoc(), dataStyle);                              // Місто/Сел. призову
                setCell(row, col++, fmt(p.getEnrollmentDate()), centerStyle);                 // Дата зарахув.
                setCell(row, col++, p.getEnrollmentNakaz(), centerStyle);                     // Наказ зарахув.
                setCell(row, col++, p.getUbdNumber(), centerStyle);                           // УБД №
                setCell(row, col++, fmt(p.getUbdDate()), centerStyle);                        // УБД дата
                setCell(row, col++, p.getAdmissionForm(), dataStyle);                         // Форма допуску
                setCell(row, col++, p.getAdmissionNakaz(), dataStyle);                        // Ф-Наказ
                setCell(row, col++, fmt(p.getAdmissionDate()), centerStyle);                  // Ф-Дата
                setCell(row, col++, p.getServiceFor(), centerStyle);                          // Служба за
                setCell(row, col++, p.getShoeSize(), centerStyle);                            // Розм. взуття
                setCell(row, col++, p.getUniformSize(), centerStyle);                         // Розм. форми
                setCell(row, col++, p.getHeadwearSize(), centerStyle);                        // Розм. гол. убору
                setCell(row, col++, weaponInfo, dataStyle);                                   // Зброя
                setCell(row, col++, p.getNote(), dataStyle);                                  // Примітка
            }

            // ===== АРКУШ 2: Освіта (всі записи) =====
            XSSFSheet eduSheet = wb.createSheet("Освіта");
            String[] EDU_HEADERS = {"№", "ПІБ", "Рівень", "Заклад", "Спеціальність", "Початок", "Кінець", "Диплом №"};
            int[] EDU_WIDTHS = {6, 25, 16, 30, 25, 14, 14, 16};
            XSSFRow eduHead = eduSheet.createRow(0);
            for (int c = 0; c < EDU_HEADERS.length; c++) {
                eduSheet.setColumnWidth(c, EDU_WIDTHS[c] * 256);
                XSSFCell cell = eduHead.createCell(c);
                cell.setCellValue(EDU_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }
            int eduRow = 1;
            for (Personnel p : list) {
                List<PersonnelEducation> edus = eduRepo.findByPersonnelIdOrderByStartDateAsc(p.getId());
                for (PersonnelEducation e : edus) {
                    XSSFRow row = eduSheet.createRow(eduRow++);
                    setCell(row, 0, eduRow - 1, centerStyle);
                    setCell(row, 1, p.getFullName(), dataStyle);
                    setCell(row, 2, e.getLevel(), dataStyle);
                    setCell(row, 3, e.getInstitution(), dataStyle);
                    setCell(row, 4, e.getSpeciality(), dataStyle);
                    setCell(row, 5, fmt(e.getStartDate()), centerStyle);
                    setCell(row, 6, fmt(e.getEndDate()), centerStyle);
                    setCell(row, 7, e.getDiploma(), centerStyle);
                }
            }

            // ===== АРКУШ 3: Діти =====
            XSSFSheet childSheet = wb.createSheet("Діти");
            String[] CHILD_HEADERS = {"№", "ПІБ батька/матері", "ПІБ дитини", "Дата народження"};
            int[] CHILD_WIDTHS = {6, 30, 30, 16};
            XSSFRow childHead = childSheet.createRow(0);
            for (int c = 0; c < CHILD_HEADERS.length; c++) {
                childSheet.setColumnWidth(c, CHILD_WIDTHS[c] * 256);
                XSSFCell cell = childHead.createCell(c);
                cell.setCellValue(CHILD_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }
            int childRow = 1;
            for (Personnel p : list) {
                List<PersonnelChild> children = childRepo.findByPersonnelIdOrderByBirthDateAsc(p.getId());
                for (PersonnelChild c : children) {
                    XSSFRow row = childSheet.createRow(childRow++);
                    setCell(row, 0, childRow - 1, centerStyle);
                    setCell(row, 1, p.getFullName(), dataStyle);
                    setCell(row, 2, c.getFullName(), dataStyle);
                    setCell(row, 3, fmt(c.getBirthDate()), centerStyle);
                }
            }

            // ===== АРКУШ 4: Озброєння =====
            XSSFSheet weaponSheet = wb.createSheet("Озброєння");
            String[] WEAPON_HEADERS = {"№", "ПІБ", "Тип зброї", "Серійний №", "Дата видачі", "Примітка"};
            int[] WEAPON_WIDTHS = {6, 30, 18, 16, 14, 25};
            XSSFRow weaponHead = weaponSheet.createRow(0);
            for (int c = 0; c < WEAPON_HEADERS.length; c++) {
                weaponSheet.setColumnWidth(c, WEAPON_WIDTHS[c] * 256);
                XSSFCell cell = weaponHead.createCell(c);
                cell.setCellValue(WEAPON_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }
            int weaponRow = 1;
            for (Personnel p : list) {
                List<PersonnelWeapon> weapons = weaponRepo.findByPersonnelId(p.getId());
                for (PersonnelWeapon w : weapons) {
                    XSSFRow row = weaponSheet.createRow(weaponRow++);
                    setCell(row, 0, weaponRow - 1, centerStyle);
                    setCell(row, 1, p.getFullName(), dataStyle);
                    setCell(row, 2, w.getWeaponType(), dataStyle);
                    setCell(row, 3, w.getSerialNumber(), centerStyle);
                    setCell(row, 4, w.getIssuedDate(), centerStyle);
                    setCell(row, 5, w.getNote(), dataStyle);
                }
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