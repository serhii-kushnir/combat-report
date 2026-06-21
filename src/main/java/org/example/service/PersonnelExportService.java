package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.example.entity.PreviousService;

@Service
public class PersonnelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PersonnelService personnelService;
    private final PersonnelEducationRepository eduRepo;
    private final PersonnelChildRepository childRepo;
    private final PersonnelWeaponRepository weaponRepo;
    private final PersonnelVosTrainingRepository vosTrainingRepo;
    private final PreviousServiceRepository previousServiceRepo;

    public PersonnelExportService(PersonnelService personnelService,
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

    // ==================== ЕКСПОРТ ВСІХ ОСІБ ====================

    public byte[] exportToXlsx() throws Exception {
        List<Personnel> list = personnelService.getAll();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ===== ЛИСТ 1: Розширена відомість =====
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

            // Заголовки
            XSSFRow hRow = sheet.createRow(0);
            hRow.setHeightInPoints(30);
            for (int c = 0; c < HEADERS.length; c++) {
                sheet.setColumnWidth(c, WIDTHS[c] * 256);
                XSSFCell cell = hRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            // Дані
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

    // ==================== ЕКСПОРТ ОДНІЄЇ ОСОБИ ====================

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

            // Аркуш 1: Основна інформація
            buildPersonSheet(wb, p);

            // Аркуш 2: Освіта
            buildEducationSheet(wb, p, edus);

            // Аркуш 3: Діти
            if (!children.isEmpty()) {
                buildChildrenSheet(wb, p, children);
            }

            // Аркуш 4: Зброя
            if (!weapons.isEmpty()) {
                buildWeaponsSheet(wb, p, weapons);
            }

            // Аркуш 5: ВОС навчання
            if (!vosTrainings.isEmpty()) {
                buildVosTrainingSheet(wb, p, vosTrainings);
            }

            // Аркуш 6: Попередня служба
            if (!prevServices.isEmpty()) {
                buildPrevServiceSheet(wb, p, prevServices);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==================== МЕТОДИ ПОБУДОВИ АРКУШІВ ДЛЯ ОДНІЄЇ ОСОБИ ====================

    private void buildPersonSheet(XSSFWorkbook wb, Personnel p) {
        XSSFSheet sheet = wb.createSheet("Основна");

        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        XSSFCellStyle centerStyle = createCenterStyle(wb);
        XSSFCellStyle leftStyle = createLeftAlignedDataStyle(wb);

        String[] headers = {
                "№", "Прізвище", "Ім'я", "По батькові", "Звання", "Коротка посада", "Повна посада",
                "Телефон", "Дата народження", "Повних років", "Ідентифікаційний код",
                "Паспорт (серія, номер)", "Група крові",
                "Водійське посвідчення (серія, номер, категорія)",
                "Адреса реєстрації", "Адреса проживання",
                "Сімейний стан", "Дружина/Чоловік", "Адреса проживання (сім'я)",
                "Дата призову", "Ким призваний", "Вид служби", "УБД №",
                "Форма допуску", "Зарахування", "Військова служба за",
                "ВОС", "Тарифний розряд", "Розмір взуття", "Розмір форми",
                "Розмір головного убору", "Примітка"
        };

        // ВИПРАВЛЕНО: додано останній елемент 30 для "Примітка"
        int[] widths = {6, 18, 14, 18, 15, 20, 30, 14, 14, 10, 14, 16, 10,
                20, 30, 30, 16, 20, 30, 14, 20, 14, 14, 20, 20, 14, 14, 10, 10, 10, 30, 30};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        XSSFRow row = sheet.createRow(1);
        row.setHeightInPoints(20);

        int col = 0;
        setCell(row, col++, 1, centerStyle);
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
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
        setCell(row, col++, passport, centerStyle);
        setCell(row, col++, p.getBloodGroup(), centerStyle);
        String driver = Stream.of(p.getDriverLicenseSeries(), p.getDriverLicenseNumber(), p.getDriverLicenseCategory())
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
        setCell(row, col++, driver, centerStyle);
        setCell(row, col++, p.getRegistrationAddress(), dataStyle);
        setCell(row, col++, p.getLivingAddress(), dataStyle);
        setCell(row, col++, p.getMaritalStatus(), centerStyle);
        setCell(row, col++, p.getSpouseName(), dataStyle);
        setCell(row, col++, p.getFamilyAddress(), dataStyle);
        setCell(row, col++, fmt(p.getDraftDate()), centerStyle);
        setCell(row, col++, p.getDraftOrganization(), dataStyle);
        setCell(row, col++, p.getServiceType(), centerStyle);
        setCell(row, col++, p.getUbdNumber(), centerStyle);
        setCell(row, col++, p.getAdmissionForm(), dataStyle);
        setCell(row, col++, p.getEnrollmentInfo(), dataStyle);
        setCell(row, col++, p.getServiceFor(), centerStyle);
        setCell(row, col++, p.getVos(), centerStyle);
        setCell(row, col++, p.getTariffGrade(), centerStyle);
        setCell(row, col++, p.getShoeSize(), centerStyle);
        setCell(row, col++, p.getUniformSize(), centerStyle);
        setCell(row, col++, p.getHeadwearSize(), centerStyle);
        setCell(row, col++, p.getNote(), dataStyle);
    }

    private void buildEducationSheet(XSSFWorkbook wb, Personnel p, List<PersonnelEducation> edus) {
        XSSFSheet sheet = wb.createSheet("Освіта");
        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        XSSFCellStyle centerStyle = createCenterStyle(wb);

        String[] headers = {"Рівень", "Заклад", "Спеціальність", "Початок", "Кінець", "Диплом №"};
        int[] widths = {16, 30, 25, 14, 14, 16};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        if (edus.isEmpty()) {
            XSSFRow emptyRow = sheet.createRow(rowNum);
            setCell(emptyRow, 0, "Немає записів", dataStyle);
        } else {
            for (PersonnelEducation e : edus) {
                XSSFRow row = sheet.createRow(rowNum++);
                setCell(row, 0, e.getLevel(), dataStyle);
                setCell(row, 1, e.getInstitution(), dataStyle);
                setCell(row, 2, e.getSpeciality(), dataStyle);
                setCell(row, 3, fmt(e.getStartDate()), centerStyle);
                setCell(row, 4, fmt(e.getEndDate()), centerStyle);
                setCell(row, 5, e.getDiploma(), centerStyle);
            }
        }
    }

    private void buildChildrenSheet(XSSFWorkbook wb, Personnel p, List<PersonnelChild> children) {
        XSSFSheet sheet = wb.createSheet("Діти");
        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        XSSFCellStyle centerStyle = createCenterStyle(wb);

        String[] headers = {"ПІБ дитини", "Дата народження", "Повних років"};
        int[] widths = {30, 16, 10};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (PersonnelChild c : children) {
            XSSFRow row = sheet.createRow(rowNum++);
            setCell(row, 0, c.getFullName(), dataStyle);
            setCell(row, 1, fmt(c.getBirthDate()), centerStyle);
            setCell(row, 2, c.getBirthDate() != null ? String.valueOf(calcAge(c.getBirthDate())) : "", centerStyle);
        }
    }

    private void buildWeaponsSheet(XSSFWorkbook wb, Personnel p, List<PersonnelWeapon> weapons) {
        XSSFSheet sheet = wb.createSheet("Зброя");
        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        XSSFCellStyle centerStyle = createCenterStyle(wb);

        String[] headers = {"Модель", "Серійний №", "Штик-багнет", "Магазини", "Калібр", "Дата видачі", "Примітка"};
        int[] widths = {18, 16, 12, 10, 10, 14, 25};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (PersonnelWeapon w : weapons) {
            XSSFRow row = sheet.createRow(rowNum++);
            setCell(row, 0, w.getWeaponType(), dataStyle);
            setCell(row, 1, w.getSerialNumber(), centerStyle);
            setCell(row, 2, w.getBayonet(), centerStyle);
            setCell(row, 3, w.getMagazines(), centerStyle);
            setCell(row, 4, w.getCaliber(), centerStyle);
            setCell(row, 5, w.getIssuedDate(), centerStyle);
            setCell(row, 6, w.getNote(), dataStyle);
        }
    }

    private void buildVosTrainingSheet(XSSFWorkbook wb, Personnel p, List<PersonnelVosTraining> trainings) {
        XSSFSheet sheet = wb.createSheet("ВОС навчання");
        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        XSSFCellStyle centerStyle = createCenterStyle(wb);

        String[] headers = {"Найменування", "Спеціальність", "ВОС №", "Розпочато", "Закінчено", "№ наказу", "Дата наказу", "№ В/Ч"};
        int[] widths = {25, 20, 12, 14, 14, 14, 14, 14};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (PersonnelVosTraining t : trainings) {
            XSSFRow row = sheet.createRow(rowNum++);
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

    private void buildPrevServiceSheet(XSSFWorkbook wb, Personnel p, List<PreviousService> services) {
        XSSFSheet sheet = wb.createSheet("Попередня служба");
        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle dataStyle = createDataStyle(wb);
        XSSFCellStyle centerStyle = createCenterStyle(wb);

        String[] headers = {"Служба", "Ким призваний", "Початок періоду", "Кінець періоду", "Звання", "Військова частина"};
        int[] widths = {18, 20, 14, 14, 14, 18};

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (PreviousService s : services) {
            XSSFRow row = sheet.createRow(rowNum++);
            setCell(row, 0, s.getServiceType(), dataStyle);
            setCell(row, 1, s.getDraftedBy(), dataStyle);
            setCell(row, 2, fmt(s.getStartDate()), centerStyle);
            setCell(row, 3, fmt(s.getEndDate()), centerStyle);
            setCell(row, 4, s.getRank(), centerStyle);
            setCell(row, 5, s.getMilitaryUnit(), dataStyle);
        }
    }

    // ==================== ДОПОМІЖНІ МЕТОДИ ====================

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

    private void setCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) return;
        if (value instanceof Integer) cell.setCellValue((Integer) value);
        else if (value instanceof Double) cell.setCellValue((Double) value);
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

    private XSSFCellStyle createLeftAlignedDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = createDataStyle(wb);
        s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private void setBorders(XSSFCellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}