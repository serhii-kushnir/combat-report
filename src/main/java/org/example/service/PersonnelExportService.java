package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.Personnel;
import org.example.entity.PersonnelChild;
import org.example.entity.PersonnelEducation;
import org.example.entity.PersonnelWeapon;
import org.example.repository.PersonnelChildRepository;
import org.example.repository.PersonnelEducationRepository;
import org.example.repository.PersonnelWeaponRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

            // ===== Лист 1: Відомість ОС =====
            XSSFSheet sheet = wb.createSheet("Відомість ОС");

            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle dataStyle   = createDataStyle(wb);
            XSSFCellStyle centerStyle = createCenterStyle(wb);

            String[] HEADERS = {
                "№", "Прізвище", "Ім'я", "По батькові", "Звання", "Посада",
                "Телефон", "Дата народження", "ІПН", "Пас. серія", "Пас. номер",
                "Група крові", "Вод. посв.", "Адреса реєстрації", "Адреса проживання",
                "Сімейний стан", "Дружина/Чоловік",
                "Дата призову", "Вид служби", "Ким призваний", "УБД №",
                "Примітка"
            };
            int[] WIDTHS = {
                6, 18, 14, 18, 18, 20,
                14, 14, 14, 10, 12,
                10, 10, 30, 30,
                14, 20,
                14, 14, 20, 12,
                30
            };

            // Заголовок
            XSSFRow hRow = sheet.createRow(0);
            hRow.setHeightInPoints(30);
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
                row.setHeightInPoints(20);

                setCell(row, 0,  i + 1, centerStyle);
                setCell(row, 1,  p.getLastName(), dataStyle);
                setCell(row, 2,  p.getFirstName(), dataStyle);
                setCell(row, 3,  p.getMiddleName(), dataStyle);
                setCell(row, 4,  p.getRank(), dataStyle);
                setCell(row, 5,  p.getPosition(), dataStyle);
                setCell(row, 6,  p.getPhone(), centerStyle);
                setCell(row, 7,  fmt(p.getBirthDate()), centerStyle);
                setCell(row, 8,  p.getTaxId(), centerStyle);
                setCell(row, 9,  p.getPassportSeries(), centerStyle);
                setCell(row, 10, p.getPassportNumber(), centerStyle);
                setCell(row, 11, p.getBloodGroup(), centerStyle);
                setCell(row, 12, p.getDriverLicense(), centerStyle);
                setCell(row, 13, p.getRegistrationAddress(), dataStyle);
                setCell(row, 14, p.getLivingAddress(), dataStyle);
                setCell(row, 15, p.getMaritalStatus(), centerStyle);
                setCell(row, 16, p.getSpouseName(), dataStyle);
                setCell(row, 17, fmt(p.getDraftDate()), centerStyle);
                setCell(row, 18, p.getServiceType(), centerStyle);
                setCell(row, 19, p.getDraftOrganization(), dataStyle);
                setCell(row, 20, p.getUbdNumber(), centerStyle);
                setCell(row, 21, p.getNote(), dataStyle);
            }

            // ===== Лист 2: Освіта =====
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

            // ===== Лист 3: Діти =====
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

            // ===== Лист 4: Озброєння =====
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

    // ===== ХЕЛПЕРИ =====
    private String fmt(java.time.LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "";
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

    private void setBorders(XSSFCellStyle s) {
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);  s.setBorderRight(BorderStyle.THIN);
    }
}