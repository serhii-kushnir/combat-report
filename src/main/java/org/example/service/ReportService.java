package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.CombatReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final ObjectMapper objectMapper;

    public ReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CombatReport parseJson(String json) throws Exception {
        CombatReport report = objectMapper.readValue(json, CombatReport.class);
        log.debug("JSON розпарсено: unitName={}, targetType={}, weaponId={}",
                report.getUnitName(), report.getTargetType(), report.getWeaponId());
        return report;
    }

    // ========== СЛОВНИК ЗАМІН ТИПІВ ЦІЛЕЙ ==========
    private static final Map<String, String> TARGET_TYPE_MAPPINGS = new HashMap<>();

    static {
        TARGET_TYPE_MAPPINGS.put("Шахед (Герань)", "Шахід");
        TARGET_TYPE_MAPPINGS.put("Shahed (Geran)", "Шахід");
        TARGET_TYPE_MAPPINGS.put("шахед", "Шахід");
        TARGET_TYPE_MAPPINGS.put("SHAHED", "Шахід");
        TARGET_TYPE_MAPPINGS.put("БпЛА - крило", "БпЛА крило");
        TARGET_TYPE_MAPPINGS.put("БпЛА крило", "БпЛА крило");
        TARGET_TYPE_MAPPINGS.put("Крилата ракета", "КР");
        TARGET_TYPE_MAPPINGS.put("Крилата ракета (КР)", "КР");
        TARGET_TYPE_MAPPINGS.put("Балістична ракета", "БР");
        TARGET_TYPE_MAPPINGS.put("Балістична ракета (БР)", "БР");
        TARGET_TYPE_MAPPINGS.put("Літак", "Літак");
        TARGET_TYPE_MAPPINGS.put("Винищувач", "Літак");
        TARGET_TYPE_MAPPINGS.put("Штурмовик", "Літак");
        TARGET_TYPE_MAPPINGS.put("Гелікоптер", "Гелікоптер");
        TARGET_TYPE_MAPPINGS.put("Вертоліт", "Гелікоптер");
    }

    private String extractWeaponName(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) {
            log.warn("weaponId порожній, використовується значення за замовчуванням");
            return "AS3 Merops";
        }
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(weaponId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return weaponId;
    }

    private String getTargetTypeDisplay(CombatReport report) {
        String targetType = report.getTargetSubType();
        if (targetType == null || targetType.isEmpty()) {
            targetType = report.getTargetType();
        }
        if (targetType == null || targetType.isEmpty()) {
            log.warn("Тип цілі не вказаний у звіті");
            return "Невідомо";
        }
        for (Map.Entry<String, String> entry : TARGET_TYPE_MAPPINGS.entrySet()) {
            if (targetType.equalsIgnoreCase(entry.getKey()) ||
                    targetType.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        log.debug("Тип цілі '{}' не знайдено в словнику, використовується оригінальне значення", targetType);
        return targetType.substring(0, 1).toUpperCase() + targetType.substring(1).toLowerCase();
    }

    private String getTargetTypeForReport(CombatReport report) {
        String targetType = getTargetTypeDisplay(report);
        if ("Шахід".equals(targetType)) {
            return "\"" + targetType + "\"";
        }
        return targetType;
    }

    private int safeInt(Integer value, int defaultValue) {
        return (value != null) ? value : defaultValue;
    }

    private String safeTargetNumber(CombatReport report) {
        Integer virazh = report.getTargetNumberVirazh();
        return (virazh != null && virazh != 0) ? String.valueOf(virazh) : "";
    }

    // ========== ФОРМАТ 1: СТАНДАРТНИЙ ==========
    public String formatStandardReport(CombatReport report, int manualDistance, int manualSpeed) {
        log.debug("Формування стандартного звіту для екіпажу: {}", report.getUnitName());
        StringBuilder sb = new StringBuilder();

        String takeoffTime = "";
        String lossTime;
        String reportDate;
        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "";

        if (report.getTakeoffTime() != null) {
            takeoffTime = report.getTakeoffTime().format(TIME_FORMATTER);
        }
        if (report.getContactTime() != null) {
            reportDate = report.getContactTime().format(DATE_FORMATTER);
            lossTime = report.getContactTime().format(TIME_FORMATTER);
        } else {
            log.warn("contactTime не вказано, використовується поточний час");
            reportDate = LocalDate.now().format(DATE_FORMATTER);
            lossTime = LocalTime.now().format(TIME_FORMATTER);
        }
        if (takeoffTime.isEmpty()) {
            takeoffTime = lossTime;
        }

        sb.append(reportDate).append("\n");
        sb.append("Екіпаж: ").append(report.getUnitName()).append("\n");

        String effectorStatus = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        sb.append(effectorStatus).append("\n\n");

        sb.append("Час: ").append(takeoffTime).append(" - ").append(lossTime).append("\n");

        String coordinates = report.getCoordinates() != null ? report.getCoordinates() : "";
        sb.append("Координати: ").append(coordinates).append("\n");
        sb.append("Відстань від місця взльоту: ").append(manualDistance).append(" м\n");
        sb.append("Тип: ").append(getTargetTypeDisplay(report)).append("\n");
        sb.append("Ідентифікація: Дружній\n");

        String weapon = extractWeaponName(report.getWeaponId());
        sb.append("Засіб ураження: ").append(weapon)
                .append(" (нічний) \"").append(weaponNumber.toUpperCase()).append("\"").append("\n");
        sb.append("Вибухівка: ШИФР «3-1.2 КУФ» 1,2 кг\n");
        sb.append("Детонатор: Вбудована розумна плата ініціації.\n");

        String unitName = report.getUnitName() != null ? report.getUnitName() : "";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "";
        String targetNumber = safeTargetNumber(report);
        String effectorStatusForNote = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        sb.append("Примітка: Екіпажем ").append("\"").append(unitName).append("\"")
                .append(" в/ч ").append(militaryUnit)
                .append(", який виконує завдання ведення повітряної розвідки та ураження противника в смузі відповідальності ОТУ м. Одеса")
                .append(" здійснено виліт дроном-камікадзе ").append("\"").append(weapon).append(" (нічний)\"")
                .append(" з метою ураження ворожого ударного дрона №").append(targetNumber)
                .append(". ").append(effectorStatusForNote)
                .append(", ").append(effectorLossReason.toLowerCase()).append("\n");

        int altitude = safeInt(report.getAltitude(), 500);
        int targetNum = safeInt(report.getTargetNumberVirazh(), 0);
        String targetSubTypeDisplay = getTargetTypeDisplay(report);

        sb.append("Висота: ").append(altitude).append("м, ціль ").append("\"").append(targetSubTypeDisplay).append("\"");
        if (targetNum != 0) {
            sb.append(" №").append(targetNum).append(" (по віражу)");
        }
        sb.append(", швидкість цілі: ").append(manualSpeed).append(" км/год.");

        return sb.toString();
    }

    // ========== ФОРМАТ 2: СКОРОЧЕНИЙ ==========
    public String formatShortReport(CombatReport report) {
        log.debug("Формування скороченого звіту для екіпажу: {}", report.getUnitName());
        StringBuilder sb = new StringBuilder();

        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "";

        sb.append("Екіпаж: ").append(report.getUnitName()).append("\n");

        String effectorStatus = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        sb.append(effectorStatus).append("\n");

        if (report.getTakeoffTime() != null) {
            sb.append("Час вильоту: ").append(report.getTakeoffTime().format(TIME_FORMATTER)).append("\n");
        }
        if (report.getContactTime() != null) {
            sb.append("Час підриву: ").append(report.getContactTime().format(TIME_FORMATTER)).append("\n");
        }

        sb.append("Район підриву: Море\n");

        String coordinates = report.getCoordinates() != null ? report.getCoordinates() : "";
        sb.append("Приблизні координати підриву: ").append(coordinates).append("\n");
        sb.append("Тип цілі: ").append(getTargetTypeDisplay(report)).append("\n");
        sb.append("Висота цілі: ").append(safeInt(report.getAltitude(), 0)).append(" метрів\n");

        String weapon = extractWeaponName(report.getWeaponId());
        sb.append("Засіб ураження: ").append(weapon)
                .append(" (нічний) \"").append(weaponNumber.toUpperCase()).append("\"").append("\n");

        int virazhNum = safeInt(report.getTargetNumberVirazh(), 0);
        sb.append("Номер цілі по Віражу: ").append(virazhNum).append("\n");

        String skymap = report.getTargetNumberSkymap() != null && !report.getTargetNumberSkymap().isEmpty()
                ? report.getTargetNumberSkymap() : String.valueOf(virazhNum);
        sb.append("Номер по СкайМаті: ").append(skymap).append("\n");

        return sb.toString();
    }

    // ========== ФОРМАТ 3: ДЕТАЛЬНИЙ (З ПІДПИСАМИ) ==========
    public String formatDetailedReport(CombatReport report, String pilot) {
        log.debug("Формування детального звіту для екіпажу: {}, пілот: {}", report.getUnitName(), pilot);
        StringBuilder sb = new StringBuilder();

        String weapon = extractWeaponName(report.getWeaponId());
        String unitName = report.getUnitName() != null ? report.getUnitName() : "СКОПА";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "А0826";
        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "";
        String targetNumber = safeTargetNumber(report);
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";
        String targetTypeDisplay = getTargetTypeForReport(report);

        boolean targetDestroyed = effectorLossReason.toLowerCase().contains("успішне") ||
                effectorLossReason.toLowerCase().contains("вражена") ||
                effectorLossReason.toLowerCase().contains("знищ") ||
                effectorLossReason.toLowerCase().contains("камікадзе");

        String reportDate;
        String takeoffTime;
        String contactTime;

        if (report.getContactTime() != null) {
            reportDate = report.getContactTime().format(DATE_FORMATTER);
            contactTime = report.getContactTime().format(TIME_FORMATTER);
        } else {
            log.warn("contactTime не вказано у детальному звіті, використовується поточний час");
            reportDate = LocalDate.now().format(DATE_FORMATTER);
            contactTime = LocalTime.now().format(TIME_FORMATTER);
        }

        takeoffTime = (report.getTakeoffTime() != null)
                ? report.getTakeoffTime().format(TIME_FORMATTER)
                : contactTime;

        String targetResult = targetDestroyed ? "вражена" : "не вражена";
        log.debug("Результат ураження цілі: {}", targetResult);

        sb.append("Командиру взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n\n\n");
        sb.append("Рапорт\n\n");
        sb.append("\tДійсним доповідаю, що ").append(reportDate)
                .append(" о ").append(takeoffTime)
                .append(" в районі м. Одеса, Одеської області, екіпажем «").append(unitName.toUpperCase())
                .append("» військової частини ").append(militaryUnit)
                .append(" здійснено пуск БпЛА \"").append(weapon).append(" (нічний)\" серійний номер ").append("\"").append(weaponNumber.toUpperCase()).append("\"")
                .append(" спорядженого тротиловою шашкою «3-1.2 КУФ» 1,2 кг та вбудованою розумною платою ініціації для виконання бойового завдання з перехоплення повітряної цілі №").append(targetNumber)
                .append(" (БпЛА противника типу ").append(targetTypeDisplay).append("). ")
                .append(reportDate).append(" о ").append(contactTime)
                .append(" БпЛА \"").append(weapon).append(" (нічний)\" серійний номер ").append("\"").append(weaponNumber.toUpperCase()).append("\"")
                .append(" споряджений тротиловою шашкою «3-1.2 КУФ» 1,2 кг та вбудованою розумною платою ініціації був витрачений у результаті контрольованого підриву для знищення повітряної цілі №").append(targetNumber)
                .append(" (БпЛА противника типу ").append(targetTypeDisplay).append("). Ціль ").append(targetResult).append(".\n\n");

        sb.append("Пілот:\n");

        if (pilot.equals("Костянтин БИТКА")) {
            sb.append("Оператор безпілотних літальних апаратів екіпажу безпілотного авіаційного комплексу\n");
            sb.append("взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
            sb.append("солдат                                                                                                           Костянтин БИТКА\n");
            sb.append(reportDate).append(" р.\n\n");
            sb.append("Командир екіпажу:\n");
            sb.append("Командир екіпажу безпілотних літальних комплексів взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
            sb.append("старший сержант                                                                                    Олександр ШЕПРУК\n");
            sb.append(reportDate).append(" р.\n\n\n\n");
        } else {
            sb.append("Командир екіпажу безпілотних літальних комплексів взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
            sb.append("старший сержант                                                                                    Олександр ШЕПРУК\n");
            sb.append(reportDate).append(" р.\n\n");
            sb.append("Командир екіпажу:\n");
            sb.append("Командир взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
            sb.append("старший лейтенант                                                                                    Микола САВЕНКО\n");
            sb.append(reportDate).append(" р.\n\n\n\n");
        }

        sb.append("Начальнику позаштатної служби безпілотних авіаційних комплексів військової частини А1620\n\n");
        sb.append("Рапорт\n\n");
        sb.append("Клопочу по суті рапорту пілота ")
                .append(pilot.equals("Костянтин БИТКА") ? "солдата Костянтина БИТКА" : "старшого сержанта Олександра ШЕПРУКА")
                .append("\n\n");
        sb.append("Командир взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
        sb.append("старший лейтенант                                                                                    Микола САВЕНКО\n");
        sb.append(reportDate).append(" р.\n");

        return sb.toString();
    }
}