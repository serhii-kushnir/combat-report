package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.CombatReport;
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

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Парсить JSON рядок в об'єкт CombatReport
     */
    public CombatReport parseJson(String json) throws Exception {
        return objectMapper.readValue(json, CombatReport.class);
    }

    // ========== СЛОВНИК ЗАМІН ТИПІВ ЦІЛЕЙ ==========
    private static final Map<String, String> TARGET_TYPE_MAPPINGS = new HashMap<>();

    static {
        TARGET_TYPE_MAPPINGS.put("Шахед (Герань)", "Шахід");
        TARGET_TYPE_MAPPINGS.put("Shahed (Geran)", "Шахід");
        TARGET_TYPE_MAPPINGS.put("шахед", "Шахід");
        TARGET_TYPE_MAPPINGS.put("SHAHED", "Шахід");
        TARGET_TYPE_MAPPINGS.put("БПЛА - крило", "БпЛА крило");
        TARGET_TYPE_MAPPINGS.put("БПЛА крило", "БпЛА крило");
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
            return "Невідомо";
        }
        for (Map.Entry<String, String> entry : TARGET_TYPE_MAPPINGS.entrySet()) {
            if (targetType.equalsIgnoreCase(entry.getKey()) ||
                    targetType.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return targetType.substring(0, 1).toUpperCase() + targetType.substring(1).toLowerCase();
    }

    private String getTargetTypeForReport(CombatReport report) {
        String targetType = getTargetTypeDisplay(report);
        if ("Шахід".equals(targetType)) {
            return "\"" + targetType + "\"";
        }
        return targetType;
    }

    // ========== ФОРМАТ 1: СТАНДАРТНИЙ ==========
    public String formatStandardReport(CombatReport report, int manualDistance, int manualSpeed) {
        StringBuilder sb = new StringBuilder();

        String takeoffTime = "";
        String lossTime = "";
        String reportDate = "";

        if (report.getTakeoffTime() != null) {
            takeoffTime = report.getTakeoffTime().format(TIME_FORMATTER);
        }

        if (report.getContactTime() != null) {
            reportDate = report.getContactTime().format(DATE_FORMATTER);
            lossTime = report.getContactTime().format(TIME_FORMATTER);
        } else {
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

        String coordinates = report.getCoordinates();
        if (coordinates == null || coordinates.isEmpty()) {
            coordinates = "36TUS3097308507";
        }
        sb.append("Координати: ").append(coordinates).append("\n");
        sb.append("Відстань від місця взльоту: ").append(manualDistance).append(" м\n");
        sb.append("Тип: ").append(getTargetTypeDisplay(report)).append("\n");
        sb.append("Ідентифікація: Дружній\n");

        String weapon = extractWeaponName(report.getWeaponId());
        sb.append("Засіб ураження: ").append(weapon)
                .append(" ").append(report.getWeaponNumber()).append("\n");

        sb.append("Вибухівка: ШИФР «3-1.2 КУФ» 1.2 кг (8g 35m H)\n");
        sb.append("Детонатор: Вбудована розумна плата ініціації.\n");

        String unitName = report.getUnitName() != null ? report.getUnitName() : "";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "";
        String geoMarker = report.getGeoMarker() != null ? report.getGeoMarker() : "";
        String targetNumber = report.getTargetNumberVirazh() != 0 ? String.valueOf(report.getTargetNumberVirazh()) : "";
        String effectorStatusForNote = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        sb.append("Примітка: Екіпажем ").append("\"").append(unitName).append("\"")
                .append(" ").append(militaryUnit)
                .append(", який виконує завдання ведення повітряної розвідки та ураження противника в смузі відповідальності ОТУ ").append(geoMarker)
                .append(", дроном - камікадзе ").append("\"").append(weapon).append("\"").append(" було здійснено виліт з метою ураження ворожого ударного дрона №").append(targetNumber)
                .append(", ").append(effectorStatusForNote)
                .append(", ").append(effectorLossReason).append("\n");

        int altitude = report.getAltitude() != 0 ? report.getAltitude() : 500;
        String targetSubTypeDisplay = getTargetTypeDisplay(report);
        int targetNum = report.getTargetNumberVirazh() != 0 ? report.getTargetNumberVirazh() : 0;

        sb.append("Висота: ").append(altitude).append("м, ціль ").append("\"").append(targetSubTypeDisplay).append("\"");
        if (targetNum != 0) {
            sb.append(" ").append("\"№").append(targetNum).append(" (по віражу)").append("\"");
        }
        sb.append(", швидкість цілі: ").append(manualSpeed).append(" км/год.");

        return sb.toString();
    }

    // ========== ФОРМАТ 2: СКОРОЧЕНИЙ ==========
    public String formatShortReport(CombatReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("Екіпаж: ").append(report.getUnitName()).append("\n");

        if (report.getTakeoffTime() != null) {
            sb.append("Час вильоту: ").append(report.getTakeoffTime().format(TIME_FORMATTER)).append("\n");
        }

        if (report.getContactTime() != null) {
            sb.append("Час підриву по цілі: ").append(report.getContactTime().format(TIME_FORMATTER)).append("\n");
        }

        String region = report.getGeoMarker() != null && !report.getGeoMarker().isEmpty()
                ? report.getGeoMarker() : "Невідомо";
        sb.append("Район підриву по цілі: ").append(region).append("\n");

        String coordinates = report.getCoordinates();
        if (coordinates == null || coordinates.isEmpty()) {
            coordinates = "36TUS3097308507";
        }
        sb.append("Приблизні координати підриву по цілі: ").append(coordinates).append("\n");

        sb.append("Тип цілі: ").append(getTargetTypeDisplay(report)).append("\n");
        sb.append("Висота цілі: ").append(report.getAltitude()).append(" метрів\n");

        String weapon = extractWeaponName(report.getWeaponId());
        sb.append("Засіб ураження: ").append(weapon)
                .append(" ").append(report.getWeaponNumber()).append("\n");

        sb.append("Номер цілі по Віражу: ").append(report.getTargetNumberVirazh()).append("\n");

        String skymap = report.getTargetNumberSkymap() != null && !report.getTargetNumberSkymap().isEmpty()
                ? report.getTargetNumberSkymap() : String.valueOf(report.getTargetNumberVirazh());
        sb.append("Номер по СкайМаті: ").append(skymap).append("\n");

        return sb.toString();
    }

    // ========== ФОРМАТ 3: ДЕТАЛЬНИЙ (З ПІДПИСАМИ) ==========
    public String formatDetailedReport(CombatReport report, String pilot) {
        StringBuilder sb = new StringBuilder();

        String weapon = extractWeaponName(report.getWeaponId());

        String unitName = report.getUnitName() != null ? report.getUnitName() : "СКОПА";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "А0826";
        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "";
        String targetNumber = report.getTargetNumberVirazh() != 0 ? String.valueOf(report.getTargetNumberVirazh()) : "";
        String geoMarker = report.getGeoMarker() != null ? report.getGeoMarker() : "Одеса";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        String targetTypeDisplay = getTargetTypeForReport(report);

        boolean targetDestroyed = effectorLossReason.toLowerCase().contains("успішне") ||
                effectorLossReason.toLowerCase().contains("вражена") ||
                effectorLossReason.toLowerCase().contains("знищ") ||
                effectorLossReason.toLowerCase().contains("камікадзе");

        String reportDate;
        String takeoffTime = "";
        String contactTime = "";

        if (report.getContactTime() != null) {
            reportDate = report.getContactTime().format(DATE_FORMATTER);
            contactTime = report.getContactTime().format(TIME_FORMATTER);
        } else {
            reportDate = LocalDate.now().format(DATE_FORMATTER);
            contactTime = LocalTime.now().format(TIME_FORMATTER);
        }

        if (report.getTakeoffTime() != null) {
            takeoffTime = report.getTakeoffTime().format(TIME_FORMATTER);
        } else {
            takeoffTime = contactTime;
        }

        String regionName = geoMarker.equals("Одеса") ? "Одеської" : geoMarker + "ської";
        String targetResult = targetDestroyed ? "вражена" : "не вражена";

        sb.append("Командиру взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n\n\n");
        sb.append("Рапорт\n\n");
        sb.append("\tДійсним доповідаю, що ").append(reportDate)
                .append(" о ").append(takeoffTime)
                .append(" в районі м.Одеса, Одеської області, екіпажем «").append(unitName.toUpperCase())
                .append("» військової частини ").append(militaryUnit)
                .append(" здійснив пуск БпЛА \"").append(weapon).append(" (нічний)\" зав. номер №").append(weaponNumber)
                .append(" спорядженого тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації для виконання бойового завдання з перехоплення повітряної цілі №").append(targetNumber)
                .append(" БПЛА противника типу ").append(targetTypeDisplay).append(". ")
                .append(reportDate).append(" о ").append(contactTime)
                .append(" БпЛА \"").append(weapon).append(" (нічний)\" зав. номер №").append(weaponNumber)
                .append(" споряджений тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації був витрачений у результаті контрольованого підриву для знищення повітряної цілі №").append(targetNumber)
                .append(" БПЛА противника типу ").append(targetTypeDisplay).append(", ціль ").append(targetResult).append(".\n\n");

        sb.append("Пілот:\n");
        sb.append("Оператор безпілотних літальних апаратів екіпажу безпілотного авіаційного комплексу\n");
        sb.append("взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");

        if (pilot.equals("Костянтин БИТКА")) {
            sb.append("солдат                                                                                                           Костянтин БИТКА\n");
        } else {
            sb.append("старший солдат                                                                                      Ярослав НАГОРНИЙ\n");
        }
        sb.append(reportDate).append(" р.\n\n");

        sb.append("Командир екіпажу:\n");
        sb.append("Командир екіпажу безпілотних літальних комплексів взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
        sb.append("старший сержант                                                                                    Олександр ШЕПРУК\n");
        sb.append(reportDate).append(" р.\n\n\n\n");

        sb.append("Начальнику позаштатної служби безпілотних авіаційних комплексів військової частини А1620\n\n");
        sb.append("Рапорт\n\n");
        sb.append("Клопочу по суті рапорту пілота ").append(pilot.equals("Костянтин БИТКА") ? "солдата Костянтина БИТКА" : "старшого солдата Ярослава НАГОРНОГО").append("\n\n");
        sb.append("Командир взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
        sb.append("старший лейтенант                                                                                    Микола САВЕНКО\n");
        sb.append(reportDate).append(" р.\n");

        return sb.toString();
    }
}