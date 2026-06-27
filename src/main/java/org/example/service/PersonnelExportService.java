package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersonnelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PersonnelEducationRepository eduRepo;
    private final PersonnelChildRepository childRepo;
    private final PersonnelWeaponRepository weaponRepo;
    private final PersonnelVosTrainingRepository vosTrainingRepo;
    private final PreviousServiceRepository previousServiceRepo;

    public PersonnelExportService(PersonnelEducationRepository eduRepo,
                                  PersonnelChildRepository childRepo,
                                  PersonnelWeaponRepository weaponRepo,
                                  PersonnelVosTrainingRepository vosTrainingRepo,
                                  PreviousServiceRepository previousServiceRepo) {
        this.eduRepo = eduRepo;
        this.childRepo = childRepo;
        this.weaponRepo = weaponRepo;
        this.vosTrainingRepo = vosTrainingRepo;
        this.previousServiceRepo = previousServiceRepo;
    }

    @Transactional(readOnly = true)
    public byte[] exportToXlsx(List<Personnel> personnelList) throws Exception {
        // Отримуємо ID осіб, які потрапили у фільтр
        Set<Long> personnelIds = personnelList.stream()
                .map(Personnel::getId)
                .collect(Collectors.toSet());

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ==================== АРКУШ 1: Відомість ОС ====================
            XSSFSheet mainSheet = wb.createSheet("Відомість ОС");
            mainSheet.setDefaultRowHeightInPoints(50);
            buildMainSheet(mainSheet, personnelList);

            // ==================== АРКУШ 2: Освіта ====================
            List<PersonnelEducation> eduList = eduRepo.findAll().stream()
                    .filter(e -> personnelIds.contains(e.getPersonnel().getId()))
                    .collect(Collectors.toList());
            if (!eduList.isEmpty()) {
                XSSFSheet eduSheet = wb.createSheet("Освіта");
                eduSheet.setDefaultRowHeightInPoints(50);
                buildEducationSheet(eduSheet, eduList);
            }

            // ==================== АРКУШ 3: Діти ====================
            List<PersonnelChild> childList = childRepo.findAll().stream()
                    .filter(c -> personnelIds.contains(c.getPersonnel().getId()))
                    .collect(Collectors.toList());
            if (!childList.isEmpty()) {
                XSSFSheet childSheet = wb.createSheet("Діти");
                childSheet.setDefaultRowHeightInPoints(50);
                buildChildSheet(childSheet, childList);
            }

            // ==================== АРКУШ 4: Зброя ====================
            List<PersonnelWeapon> weaponList = weaponRepo.findAll().stream()
                    .filter(w -> personnelIds.contains(w.getPersonnel().getId()))
                    .collect(Collectors.toList());
            if (!weaponList.isEmpty()) {
                XSSFSheet weaponSheet = wb.createSheet("Озброєння");
                weaponSheet.setDefaultRowHeightInPoints(50);
                buildWeaponSheet(weaponSheet, weaponList);
            }

            // ==================== АРКУШ 5: ВОС Навчання ====================
            List<PersonnelVosTraining> vosList = vosTrainingRepo.findAll().stream()
                    .filter(v -> personnelIds.contains(v.getPersonnel().getId()))
                    .collect(Collectors.toList());
            if (!vosList.isEmpty()) {
                XSSFSheet vosSheet = wb.createSheet("ВОС Навчання");
                vosSheet.setDefaultRowHeightInPoints(50);
                buildVosSheet(vosSheet, vosList);
            }

            // ==================== АРКУШ 6: Попередня служба ====================
            List<PreviousService> prevList = previousServiceRepo.findAll().stream()
                    .filter(p -> personnelIds.contains(p.getPersonnel().getId()))
                    .collect(Collectors.toList());
            if (!prevList.isEmpty()) {
                XSSFSheet prevSheet = wb.createSheet("Попередня служба");
                prevSheet.setDefaultRowHeightInPoints(50);
                buildPrevServiceSheet(prevSheet, prevList);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ===== ПОБУДОВА АРКУШІВ =====

    private void buildMainSheet(XSSFSheet sheet, List<Personnel> list) {
        sheet.setDefaultRowHeightInPoints(50);

        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle centerStyle = createCenterStyle(sheet.getWorkbook());

        String[] HEADERS = {
                "№", "Статус", "Звання", "ПІБ", "Телефон", "Дата народж.",
                "Вік", "ІПН", "Група крові", "Паспорт серія", "Паспорт №",
                "Вод. серія", "Вод. №", "Вод. категорія",
                "Адреса реєстрації", "Адреса проживання",
                "Сімейний стан", "Дружина/Чоловік", "Адреса сім'ї",
                "Освіта рівень", "Ступінь", "Заклад освіти", "Спеціальність",
                "Початок", "Кінець", "Диплом №",
                "Посада кор.", "Посада повна", "Військова частина",
                "ВОС", "Тариф", "Дата призову", "Ким призваний",
                "Область призову", "Місто/Сел. призову",
                "Дата зарахув.", "Наказ зарахув.",
                "УБД №", "УБД дата",
                "Форма допуску", "Ф-Наказ", "Ф-Дата",
                "Служба за", "Розм. взуття", "Розм. форми", "Розм. гол. убору",
                "Модель зброї", "Серійний №", "Штик-багнет", "Магазини", "Калібр", "Дата видачі",
                "Кількість дітей",
                "Примітка"
        };

        int[] WIDTHS = {
                6, 14, 16, 30, 16, 16, 6, 16, 14, 16, 16,
                16, 16, 16,
                30, 30,
                16, 24, 30,
                16, 14, 30, 24,
                14, 14, 16,
                20, 30, 18,
                12, 12, 14, 24,
                18, 18,
                16, 18,
                16, 14,
                16, 16, 16,
                16, 12, 12, 14,
                16, 16, 16, 16, 16, 16,
                10,
                30
        };

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(50);
        for (int c = 0; c < HEADERS.length; c++) {
            sheet.setColumnWidth(c, WIDTHS[c] * 256);
            XSSFCell cell = headerRow.createCell(c);
            cell.setCellValue(HEADERS[c]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int seq = 1;
        for (Personnel p : list) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(50);
            int col = 0;
            setCell(row, col++, seq++, centerStyle);
            setCell(row, col++, p.getPersonnelStatus(), centerStyle);
            setCell(row, col++, p.getRank(), centerStyle);
            setCell(row, col++, p.getFullName(), dataStyle);
            setCell(row, col++, p.getPhone(), centerStyle);
            setCell(row, col++, fmt(p.getBirthDate()), centerStyle);
            setCell(row, col++, p.getAge() > 0 ? String.valueOf(p.getAge()) : "", centerStyle);
            setCell(row, col++, p.getTaxId(), centerStyle);
            setCell(row, col++, p.getBloodGroup(), centerStyle);
            setCell(row, col++, p.getPassportSeries(), centerStyle);
            setCell(row, col++, p.getPassportNumber(), centerStyle);
            setCell(row, col++, p.getDriverLicenseSeries(), centerStyle);
            setCell(row, col++, p.getDriverLicenseNumber(), centerStyle);
            setCell(row, col++, p.getDriverLicenseCategory(), centerStyle);
            setCell(row, col++, p.getRegistrationAddress(), dataStyle);
            setCell(row, col++, p.getLivingAddress(), dataStyle);
            setCell(row, col++, p.getMaritalStatus(), centerStyle);
            setCell(row, col++, p.getSpouseName(), dataStyle);
            setCell(row, col++, p.getFamilyAddress(), dataStyle);
            setCell(row, col++, p.getEducation(), dataStyle);
            setCell(row, col++, p.getAcademicDegree(), dataStyle);
            setCell(row, col++, p.getEducationInstitution(), dataStyle);
            setCell(row, col++, p.getEducationSpeciality(), dataStyle);
            setCell(row, col++, fmtDate(p.getEducationStart()), centerStyle);
            setCell(row, col++, fmtDate(p.getEducationEnd()), centerStyle);
            setCell(row, col++, p.getDiploma(), centerStyle);
            setCell(row, col++, p.getPosition(), dataStyle);
            setCell(row, col++, p.getFullPosition(), dataStyle);
            setCell(row, col++, p.getMilitaryUnit(), dataStyle);
            setCell(row, col++, p.getVos(), centerStyle);
            setCell(row, col++, p.getTariffGrade(), centerStyle);
            setCell(row, col++, fmt(p.getDraftDate()), centerStyle);
            setCell(row, col++, p.getDraftOrganization(), dataStyle);
            setCell(row, col++, p.getDrafObl(), dataStyle);
            setCell(row, col++, p.getDraftLoc(), dataStyle);
            setCell(row, col++, fmt(p.getEnrollmentDate()), centerStyle);
            setCell(row, col++, p.getEnrollmentNakaz(), centerStyle);
            setCell(row, col++, p.getUbdNumber(), centerStyle);
            setCell(row, col++, fmt(p.getUbdDate()), centerStyle);
            setCell(row, col++, p.getAdmissionForm(), dataStyle);
            setCell(row, col++, p.getAdmissionNakaz(), dataStyle);
            setCell(row, col++, fmt(p.getAdmissionDate()), centerStyle);
            setCell(row, col++, p.getServiceFor(), centerStyle);
            setCell(row, col++, p.getShoeSize(), centerStyle);
            setCell(row, col++, p.getUniformSize(), centerStyle);
            setCell(row, col++, p.getHeadwearSize(), centerStyle);
            setCell(row, col++, p.getWeaponType(), dataStyle);
            setCell(row, col++, p.getWeaponSerial(), centerStyle);
            setCell(row, col++, p.getWeaponBayonet(), centerStyle);
            setCell(row, col++, p.getWeaponMagazines(), centerStyle);
            setCell(row, col++, p.getWeaponCaliber(), centerStyle);
            setCell(row, col++, p.getWeaponIssuedDate(), centerStyle);
            setCell(row, col++, p.getChildrenCount() != null ? p.getChildrenCount() : 0, centerStyle);
            setCell(row, col++, p.getNote(), dataStyle);
        }
    }

    private void buildEducationSheet(XSSFSheet sheet, List<PersonnelEducation> list) {
        sheet.setDefaultRowHeightInPoints(50);

        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle centerStyle = createCenterStyle(sheet.getWorkbook());

        String[] headers = {"№", "ПІБ особи", "Рівень", "Ступінь", "Заклад", "Спеціальність", "Початок", "Кінець", "Диплом №"};
        int[] widths = {6, 30, 16, 16, 30, 24, 14, 14, 16};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(50);
        for (int c = 0; c < headers.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
            XSSFCell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int seq = 1;
        for (PersonnelEducation e : list) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(50);
            Personnel p = e.getPersonnel();
            setCell(row, 0, seq++, centerStyle);
            setCell(row, 1, p != null ? p.getFullName() : "", dataStyle);
            setCell(row, 2, e.getLevel(), dataStyle);
            setCell(row, 3, e.getAcademicDegree(), dataStyle);
            setCell(row, 4, e.getInstitution(), dataStyle);
            setCell(row, 5, e.getSpeciality(), dataStyle);
            setCell(row, 6, fmt(e.getStartDate()), centerStyle);
            setCell(row, 7, fmt(e.getEndDate()), centerStyle);
            setCell(row, 8, e.getDiploma(), centerStyle);
        }
    }

    private void buildChildSheet(XSSFSheet sheet, List<PersonnelChild> list) {
        sheet.setDefaultRowHeightInPoints(50);

        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle centerStyle = createCenterStyle(sheet.getWorkbook());

        String[] headers = {"№", "ПІБ особи", "ПІБ дитини", "Дата народження"};
        int[] widths = {6, 30, 30, 16};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(50);
        for (int c = 0; c < headers.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
            XSSFCell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int seq = 1;
        for (PersonnelChild c : list) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(50);
            Personnel p = c.getPersonnel();
            setCell(row, 0, seq++, centerStyle);
            setCell(row, 1, p != null ? p.getFullName() : "", dataStyle);
            setCell(row, 2, c.getFullName(), dataStyle);
            setCell(row, 3, fmt(c.getBirthDate()), centerStyle);
        }
    }

    private void buildWeaponSheet(XSSFSheet sheet, List<PersonnelWeapon> list) {
        sheet.setDefaultRowHeightInPoints(50);

        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle centerStyle = createCenterStyle(sheet.getWorkbook());

        String[] headers = {"№", "ПІБ особи", "Модель", "Серійний №", "Штик-багнет", "Магазини", "Калібр", "Дата видачі"};
        int[] widths = {6, 30, 18, 16, 16, 16, 16, 16};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(50);
        for (int c = 0; c < headers.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
            XSSFCell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int seq = 1;
        for (PersonnelWeapon w : list) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(50);
            Personnel p = w.getPersonnel();
            setCell(row, 0, seq++, centerStyle);
            setCell(row, 1, p != null ? p.getFullName() : "", dataStyle);
            setCell(row, 2, w.getWeaponType(), dataStyle);
            setCell(row, 3, w.getSerialNumber(), centerStyle);
            setCell(row, 4, w.getBayonet(), centerStyle);
            setCell(row, 5, w.getMagazines(), centerStyle);
            setCell(row, 6, w.getCaliber(), centerStyle);
            setCell(row, 7, w.getIssuedDate(), centerStyle);
        }
    }

    private void buildVosSheet(XSSFSheet sheet, List<PersonnelVosTraining> list) {
        sheet.setDefaultRowHeightInPoints(50);

        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle centerStyle = createCenterStyle(sheet.getWorkbook());

        String[] headers = {"№", "ПІБ особи", "Найменування", "Спеціальність", "ВОС №", "Розпочато", "Закінчено", "№ наказу", "Дата наказу", "№ В/Ч"};
        int[] widths = {6, 30, 24, 24, 16, 14, 14, 16, 16, 16};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(50);
        for (int c = 0; c < headers.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
            XSSFCell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int seq = 1;
        for (PersonnelVosTraining t : list) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(50);
            Personnel p = t.getPersonnel();
            setCell(row, 0, seq++, centerStyle);
            setCell(row, 1, p != null ? p.getFullName() : "", dataStyle);
            setCell(row, 2, t.getName(), dataStyle);
            setCell(row, 3, t.getSpeciality(), dataStyle);
            setCell(row, 4, t.getVosNumber(), centerStyle);
            setCell(row, 5, fmt(t.getStartDate()), centerStyle);
            setCell(row, 6, fmt(t.getEndDate()), centerStyle);
            setCell(row, 7, t.getOrderNumber(), centerStyle);
            setCell(row, 8, fmt(t.getOrderDate()), centerStyle);
            setCell(row, 9, t.getMilitaryUnit(), centerStyle);
        }
    }

    private void buildPrevServiceSheet(XSSFSheet sheet, List<PreviousService> list) {
        sheet.setDefaultRowHeightInPoints(50);

        XSSFCellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        XSSFCellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        XSSFCellStyle centerStyle = createCenterStyle(sheet.getWorkbook());

        String[] headers = {"№", "ПІБ особи", "Служба", "Ким призваний", "Початок", "Кінець", "Звання", "Військова частина"};
        int[] widths = {6, 30, 24, 24, 14, 14, 16, 20};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(50);
        for (int c = 0; c < headers.length; c++) {
            sheet.setColumnWidth(c, widths[c] * 256);
            XSSFCell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        int seq = 1;
        for (PreviousService s : list) {
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(50);
            Personnel p = s.getPersonnel();
            setCell(row, 0, seq++, centerStyle);
            setCell(row, 1, p != null ? p.getFullName() : "", dataStyle);
            setCell(row, 2, s.getServiceType(), dataStyle);
            setCell(row, 3, s.getDraftedBy(), dataStyle);
            setCell(row, 4, fmt(s.getStartDate()), centerStyle);
            setCell(row, 5, fmt(s.getEndDate()), centerStyle);
            setCell(row, 6, s.getRank(), centerStyle);
            setCell(row, 7, s.getMilitaryUnit(), dataStyle);
        }
    }

    // ===== ДОПОМІЖНІ МЕТОДИ =====

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

    private String fmtDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            LocalDate d = LocalDate.parse(dateStr);
            return d.format(DATE_FMT);
        } catch (Exception e) {
            return dateStr;
        }
    }
}