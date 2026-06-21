package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.example.entity.PreviousService;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class PCardExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PersonnelService personnelService;
    private final PersonnelEducationRepository eduRepo;
    private final PersonnelChildRepository childRepo;
    private final PersonnelWeaponRepository weaponRepo;
    private final PersonnelVosTrainingRepository vosTrainingRepo;
    private final PreviousServiceRepository previousServiceRepo;

    public PCardExportService(PersonnelService personnelService,
                              PersonnelEducationRepository eduRepo,
                              PersonnelChildRepository childRepo,
                              PersonnelWeaponRepository weaponRepo,
                              PersonnelVosTrainingRepository vosTrainingRepo,
                              PreviousServiceRepository previousServiceRepo) {
        this.personnelService = personnelService;
        this.eduRepo = eduRepo;
        this.childRepo = childRepo;
        this.weaponRepo = weaponRepo;
        this.vosTrainingRepo = vosTrainingRepo;
        this.previousServiceRepo = previousServiceRepo;
    }

    public byte[] exportPersonToXlsx(Long id) throws Exception {
        Personnel p = personnelService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        List<PersonnelEducation> edus = eduRepo.findByPersonnelIdOrderByStartDateAsc(id);
        List<PersonnelChild> children = childRepo.findByPersonnelIdOrderByBirthDateAsc(id);
        List<PersonnelWeapon> weapons = weaponRepo.findByPersonnelId(id);
        List<PersonnelVosTraining> vosTrainings = vosTrainingRepo.findByPersonnelIdOrderByStartDateAsc(id);
        List<PreviousService> prevServices = previousServiceRepo.findByPersonnelIdOrderByStartDateAsc(id);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Картка особи");

            // Створюємо стилі
            XSSFCellStyle categoryStyle = createCategoryStyle(wb);
            XSSFCellStyle labelStyle = createLabelStyle(wb);
            XSSFCellStyle valueStyle = createValueStyle(wb);
            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle = createDataStyle(wb);
            XSSFCellStyle centerStyle = createCenterStyle(wb);

            // Встановлюємо ширини колонок
            sheet.setColumnWidth(0, 35 * 256);
            sheet.setColumnWidth(1, 60 * 256);

            int rowNum = 0;

            // ==================================================================
            // 1. № Порядковий номер та Статус
            // ==================================================================
            addCategory(sheet, rowNum++, "№ Порядковий номер та Статус", categoryStyle);
            addField(sheet, rowNum++, "Порядковий номер", p.getPersonnelNumber(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Статус", p.getPersonnelStatus(), labelStyle, valueStyle);

            // ==================================================================
            // 2. 👤 Загальна інформація
            // ==================================================================
            addCategory(sheet, rowNum++, "👤 Загальна інформація", categoryStyle);
            addField(sheet, rowNum++, "Прізвище", p.getLastName(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Ім'я", p.getFirstName(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "По батькові", p.getMiddleName(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Телефон", p.getPhone(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Дата народження", fmt(p.getBirthDate()), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Повних років", p.getBirthDate() != null ? String.valueOf(calcAge(p.getBirthDate())) : "", labelStyle, valueStyle);
            addField(sheet, rowNum++, "Ідентифікаційний код", p.getTaxId(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Група крові", p.getBloodGroup(), labelStyle, valueStyle);

            // ==================================================================
            // 3. 🪪 Паспорт
            // ==================================================================
            addCategory(sheet, rowNum++, "🪪 Паспорт", categoryStyle);
            addField(sheet, rowNum++, "Серія", p.getPassportSeries(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Номер", p.getPassportNumber(), labelStyle, valueStyle);

            // ==================================================================
            // 4. 🚗 Водійське посвідчення
            // ==================================================================
            addCategory(sheet, rowNum++, "🚗 Водійське посвідчення", categoryStyle);
            addField(sheet, rowNum++, "Серія", p.getDriverLicenseSeries(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Номер", p.getDriverLicenseNumber(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Категорія", p.getDriverLicenseCategory(), labelStyle, valueStyle);

            // ==================================================================
            // 5. 🏠 Адреса
            // ==================================================================
            addCategory(sheet, rowNum++, "🏠 Адреса", categoryStyle);
            addField(sheet, rowNum++, "Адреса реєстрації", p.getRegistrationAddress(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Адреса проживання", p.getLivingAddress(), labelStyle, valueStyle);

            // ==================================================================
            // 6. 🎓 Освіта
            // ==================================================================
            addCategory(sheet, rowNum++, "🎓 Освіта", categoryStyle);
            if (edus.isEmpty()) {
                addField(sheet, rowNum++, "Записів немає", "", labelStyle, valueStyle);
            } else {
                // Заголовки
                String[] eduHeaders = {"Рівень", "Заклад", "Спеціальність", "Початок", "Кінець", "Диплом №"};
                for (int idx = 0; idx < eduHeaders.length; idx++) {
                    sheet.setColumnWidth(idx, (eduHeaders[idx].length() + 6) * 256);
                }
                XSSFRow headerRow = sheet.createRow(rowNum++);
                headerRow.setHeightInPoints(22);
                for (int idx = 0; idx < eduHeaders.length; idx++) {
                    XSSFCell cell = headerRow.createCell(idx);
                    cell.setCellValue(eduHeaders[idx]);
                    cell.setCellStyle(headerStyle);
                }
                for (PersonnelEducation e : edus) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    setCell(row, 0, e.getLevel(), dataStyle);
                    setCell(row, 1, e.getInstitution(), dataStyle);
                    setCell(row, 2, e.getSpeciality(), dataStyle);
                    setCell(row, 3, fmt(e.getStartDate()), centerStyle);
                    setCell(row, 4, fmt(e.getEndDate()), centerStyle);
                    setCell(row, 5, e.getDiploma(), centerStyle);
                }
            }

            // ==================================================================
            // 7. 👨‍👩‍👧 Сім'я
            // ==================================================================
            addCategory(sheet, rowNum++, "👨‍👩‍👧 Сім'я", categoryStyle);
            addField(sheet, rowNum++, "Сімейний стан", p.getMaritalStatus(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Дружина/Чоловік", p.getSpouseName(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Адреса проживання (сім'я)", p.getFamilyAddress(), labelStyle, valueStyle);

            // ==================================================================
            // 8. 👶 Діти
            // ==================================================================
            addCategory(sheet, rowNum++, "👶 Діти", categoryStyle);
            if (children.isEmpty()) {
                addField(sheet, rowNum++, "Записів немає", "", labelStyle, valueStyle);
            } else {
                String[] childHeaders = {"ПІБ дитини", "Дата народження", "Повних років"};
                for (int idx = 0; idx < childHeaders.length; idx++) {
                    sheet.setColumnWidth(idx, (childHeaders[idx].length() + 6) * 256);
                }
                XSSFRow headerRow = sheet.createRow(rowNum++);
                headerRow.setHeightInPoints(22);
                for (int idx = 0; idx < childHeaders.length; idx++) {
                    XSSFCell cell = headerRow.createCell(idx);
                    cell.setCellValue(childHeaders[idx]);
                    cell.setCellStyle(headerStyle);
                }
                for (PersonnelChild c : children) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    setCell(row, 0, c.getFullName(), dataStyle);
                    setCell(row, 1, fmt(c.getBirthDate()), centerStyle);
                    setCell(row, 2, c.getBirthDate() != null ? String.valueOf(calcAge(c.getBirthDate())) : "", centerStyle);
                }
            }

            // ==================================================================
            // 9. 🎖️ Військові дані
            // ==================================================================
            addCategory(sheet, rowNum++, "🎖️ Військові дані", categoryStyle);
            addField(sheet, rowNum++, "Звання", p.getRank(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Коротка посада", p.getPosition(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Повна посада", p.getFullPosition(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Військова служба за", p.getServiceFor(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "ВОС", p.getVos(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Тарифний розряд", p.getTariffGrade(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Військова частина", p.getMilitaryUnit(), labelStyle, valueStyle);

            // ==================================================================
            // 10. 🪖 Призваний на військову службу
            // ==================================================================
            addCategory(sheet, rowNum++, "🪖 Призваний на військову службу", categoryStyle);
            addField(sheet, rowNum++, "Дата призову", fmt(p.getDraftDate()), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Ким призваний", p.getDraftOrganization(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Область", p.getDrafObl(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Місто/Селище", p.getDraftLoc(), labelStyle, valueStyle);

            // ==================================================================
            // 11. 📜 Зарахування у військову частину
            // ==================================================================
            addCategory(sheet, rowNum++, "📜 Зарахування у військову частину", categoryStyle);
            addField(sheet, rowNum++, "Дата", fmt(p.getEnrollmentDate()), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Наказ", p.getEnrollmentNakaz(), labelStyle, valueStyle);

            // ==================================================================
            // 12. 🏅 Учасник бойових дій
            // ==================================================================
            addCategory(sheet, rowNum++, "🏅 Учасник бойових дій", categoryStyle);
            addField(sheet, rowNum++, "УБД Номер", p.getUbdNumber(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "УБД Дата", fmt(p.getUbdDate()), labelStyle, valueStyle);

            // ==================================================================
            // 13. 🔑 Форма допуску
            // ==================================================================
            addCategory(sheet, rowNum++, "🔑 Форма допуску", categoryStyle);
            addField(sheet, rowNum++, "Ф-Номер", p.getAdmissionForm(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Ф-Наказ", p.getAdmissionNakaz(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Ф-Дата", fmt(p.getAdmissionDate()), labelStyle, valueStyle);

            // ==================================================================
            // 14. 🧥 Речове забезпечення
            // ==================================================================
            addCategory(sheet, rowNum++, "🧥 Речове забезпечення", categoryStyle);
            addField(sheet, rowNum++, "Розмір взуття", p.getShoeSize(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Розмір форми", p.getUniformSize(), labelStyle, valueStyle);
            addField(sheet, rowNum++, "Розмір головного убору", p.getHeadwearSize(), labelStyle, valueStyle);

            // ==================================================================
            // 15. 🔫 Закріплена зброя
            // ==================================================================
            addCategory(sheet, rowNum++, "🔫 Закріплена зброя", categoryStyle);
            if (weapons.isEmpty()) {
                addField(sheet, rowNum++, "Записів немає", "", labelStyle, valueStyle);
            } else {
                String[] weaponHeaders = {"Модель", "Серійний №", "Штик-багнет", "Магазини", "Калібр", "Дата видачі", "Примітка"};
                for (int idx = 0; idx < weaponHeaders.length; idx++) {
                    sheet.setColumnWidth(idx, (weaponHeaders[idx].length() + 6) * 256);
                }
                XSSFRow headerRow = sheet.createRow(rowNum++);
                headerRow.setHeightInPoints(22);
                for (int idx = 0; idx < weaponHeaders.length; idx++) {
                    XSSFCell cell = headerRow.createCell(idx);
                    cell.setCellValue(weaponHeaders[idx]);
                    cell.setCellStyle(headerStyle);
                }
                for (PersonnelWeapon w : weapons) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    setCell(row, 0, w.getWeaponType(), dataStyle);
                    setCell(row, 1, w.getSerialNumber(), centerStyle);
                    setCell(row, 2, w.getBayonet(), centerStyle);
                    setCell(row, 3, w.getMagazines(), centerStyle);
                    setCell(row, 4, w.getCaliber(), centerStyle);
                    setCell(row, 5, w.getIssuedDate(), centerStyle);
                    setCell(row, 6, w.getNote(), dataStyle);
                }
            }

            // ==================================================================
            // 16. 📚 ВОС (Навчання)
            // ==================================================================
            addCategory(sheet, rowNum++, "📚 ВОС (Навчання)", categoryStyle);
            if (vosTrainings.isEmpty()) {
                addField(sheet, rowNum++, "Записів немає", "", labelStyle, valueStyle);
            } else {
                String[] vosHeaders = {"Найменування", "Спеціальність", "ВОС №", "Розпочато", "Закінчено", "№ наказу", "Дата наказу", "№ В/Ч"};
                for (int idx = 0; idx < vosHeaders.length; idx++) {
                    sheet.setColumnWidth(idx, (vosHeaders[idx].length() + 6) * 256);
                }
                XSSFRow headerRow = sheet.createRow(rowNum++);
                headerRow.setHeightInPoints(22);
                for (int idx = 0; idx < vosHeaders.length; idx++) {
                    XSSFCell cell = headerRow.createCell(idx);
                    cell.setCellValue(vosHeaders[idx]);
                    cell.setCellStyle(headerStyle);
                }
                for (PersonnelVosTraining t : vosTrainings) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    setCell(row, 0, t.getName(), dataStyle);
                    setCell(row, 1, t.getSpeciality(), dataStyle);
                    setCell(row, 2, t.getVosNumber(), centerStyle);
                    setCell(row, 3, fmt(t.getStartDate()), centerStyle);
                    setCell(row, 4, fmt(t.getEndDate()), centerStyle);
                    setCell(row, 5, t.getOrderNumber(), centerStyle);
                    setCell(row, 6, fmt(t.getOrderDate()), centerStyle);
                    setCell(row, 7, t.getMilitaryUnit(), centerStyle);
                }
            }

            // ==================================================================
            // 17. 🪖 Попередня військова служба
            // ==================================================================
            addCategory(sheet, rowNum++, "🪖 Попередня військова служба", categoryStyle);
            if (prevServices.isEmpty()) {
                addField(sheet, rowNum++, "Записів немає", "", labelStyle, valueStyle);
            } else {
                String[] prevHeaders = {"Служба", "Ким призваний", "Початок періоду", "Кінець періоду", "Звання", "Військова частина"};
                for (int idx = 0; idx < prevHeaders.length; idx++) {
                    sheet.setColumnWidth(idx, (prevHeaders[idx].length() + 6) * 256);
                }
                XSSFRow headerRow = sheet.createRow(rowNum++);
                headerRow.setHeightInPoints(22);
                for (int idx = 0; idx < prevHeaders.length; idx++) {
                    XSSFCell cell = headerRow.createCell(idx);
                    cell.setCellValue(prevHeaders[idx]);
                    cell.setCellStyle(headerStyle);
                }
                for (PreviousService s : prevServices) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    setCell(row, 0, s.getServiceType(), dataStyle);
                    setCell(row, 1, s.getDraftedBy(), dataStyle);
                    setCell(row, 2, fmt(s.getStartDate()), centerStyle);
                    setCell(row, 3, fmt(s.getEndDate()), centerStyle);
                    setCell(row, 4, s.getRank(), centerStyle);
                    setCell(row, 5, s.getMilitaryUnit(), dataStyle);
                }
            }

            // ==================================================================
            // 18. 📝 Примітка
            // ==================================================================
            addCategory(sheet, rowNum++, "📝 Примітка", categoryStyle);
            addField(sheet, rowNum++, "Примітка", p.getNote(), labelStyle, valueStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==================== ДОПОМІЖНІ МЕТОДИ ДЛЯ ПОБУДОВИ АРКУША ====================

    private void addCategory(XSSFSheet sheet, int rowNum, String title, XSSFCellStyle style) {
        XSSFRow row = sheet.createRow(rowNum);
        row.setHeightInPoints(28);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 1));
    }

    private void addField(XSSFSheet sheet, int rowNum, String label, Object value,
                          XSSFCellStyle labelStyle, XSSFCellStyle valueStyle) {
        XSSFRow row = sheet.createRow(rowNum);
        row.setHeightInPoints(18);

        XSSFCell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        XSSFCell valueCell = row.createCell(1);
        String val = (value != null && !value.toString().isBlank()) ? value.toString() : "—";
        valueCell.setCellValue(val);
        valueCell.setCellStyle(valueStyle);
    }

    // ==================== СТИЛІ ====================

    private XSSFCellStyle createCategoryStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{26, 35, 126}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)245, (byte)245, (byte)245}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createValueStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style);
        return style;
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

    // ==================== БАЗОВІ ДОПОМІЖНІ МЕТОДИ ====================

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer) cell.setCellValue((Integer) value);
        else if (value instanceof Double) cell.setCellValue((Double) value);
        else if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
        else cell.setCellValue(value.toString());
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