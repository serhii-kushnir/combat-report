package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.CombatReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReportService")
class ReportServiceTest {

    private ReportService reportService;

    private static final int DISTANCE  = 5000;
    private static final int SPEED     = 160;
    private static final int COURSE    = 270;
    private static final int M_ALT     = 50;
    private static final int T_ALT     = 300;
    private static final String EXPLOSION_AREA = "Море";  // новий параметр

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        reportService = new ReportService(mapper);
    }

    // ========== parseJson ==========

    @Test
    @DisplayName("parseJson — валідний JSON повертає CombatReport")
    void parseJson_valid_returnsReport() throws Exception {
        String json = """
                {
                  "unitName": "СКОПА",
                  "militaryUnit": "А0826",
                  "targetType": "Шахед (Герань)",
                  "weaponId": "drone (AS3 Merops)",
                  "weaponNumber": "abc123",
                  "altitude": 300,
                  "targetNumberVirazh": 5
                }
                """;
        CombatReport report = reportService.parseJson(json);
        assertThat(report.getUnitName()).isEqualTo("СКОПА");
        assertThat(report.getMilitaryUnit()).isEqualTo("А0826");
        assertThat(report.getAltitude()).isEqualTo(300);
        assertThat(report.getTargetNumberVirazh()).isEqualTo(5);
    }

    @Test
    @DisplayName("parseJson — невідомі поля ігноруються")
    void parseJson_unknownFields_ignored() throws Exception {
        String json = "{\"unitName\": \"СКОПА\", \"unknownField\": \"value\"}";
        CombatReport report = reportService.parseJson(json);
        assertThat(report.getUnitName()).isEqualTo("СКОПА");
    }

    @Test
    @DisplayName("parseJson — невалідний JSON кидає виняток")
    void parseJson_invalid_throwsException() {
        assertThatThrownBy(() -> reportService.parseJson("not a json"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("parseJson — null поля (Integer) не кидають NPE")
    void parseJson_nullIntegerFields_noNpe() throws Exception {
        CombatReport report = reportService.parseJson("{\"unitName\": \"СКОПА\"}");
        assertThat(report.getAltitude()).isNull();
        assertThat(report.getTargetNumberVirazh()).isNull();
    }

    // ========== formatStandardReport ==========

    @Test
    @DisplayName("formatStandardReport — містить ім'я екіпажу")
    void formatStandard_containsUnitName() {
        String result = reportService.formatStandardReport(buildReport(), DISTANCE, SPEED, COURSE, M_ALT, T_ALT);
        assertThat(result).contains("СКОПА");
    }

    @Test
    @DisplayName("formatStandardReport — містить відстань і швидкість")
    void formatStandard_containsDistanceAndSpeed() {
        String result = reportService.formatStandardReport(buildReport(), 7500, 200, COURSE, M_ALT, T_ALT);
        assertThat(result).contains("7500");
        assertThat(result).contains("200 км/год");
    }

    @Test
    @DisplayName("formatStandardReport — містить курс і висоту")
    void formatStandard_containsCourseAndAltitude() {
        String result = reportService.formatStandardReport(buildReport(), DISTANCE, SPEED, 270, 50, 350);
        assertThat(result).contains("270");
        assertThat(result).contains("350");
    }

    @Test
    @DisplayName("formatStandardReport — Шахед відображається як Шахід")
    void formatStandard_shahedMappedToShahid() {
        CombatReport report = buildReport();
        report.setTargetSubType("Шахед (Герань)");
        String result = reportService.formatStandardReport(report, DISTANCE, SPEED, COURSE, M_ALT, T_ALT);
        assertThat(result).contains("Шахід");
    }

    @Test
    @DisplayName("formatStandardReport — targetNumberVirazh null не кидає NPE")
    void formatStandard_nullVirazhNumber_noNpe() {
        CombatReport report = buildReport();
        report.setTargetNumberVirazh(null);
        String result = reportService.formatStandardReport(report, DISTANCE, SPEED, COURSE, M_ALT, T_ALT);
        assertThat(result).doesNotContain("по віражу");
    }

    @Test
    @DisplayName("formatStandardReport — targetNumberVirazh показується якщо ненульовий")
    void formatStandard_virazhNumber_shown() {
        CombatReport report = buildReport();
        report.setTargetNumberVirazh(42);
        String result = reportService.formatStandardReport(report, DISTANCE, SPEED, COURSE, M_ALT, T_ALT);
        assertThat(result).contains("№42").contains("по віражу");
    }

    // ========== formatShortReport (оновлено з explosionArea) ==========

    @Test
    @DisplayName("formatShortReport — містить номер зброї у верхньому регістрі")
    void formatShort_weaponNumberUpperCase() {
        CombatReport report = buildReport();
        report.setWeaponNumber("abc-123");
        String result = reportService.formatShortReport(report, DISTANCE, COURSE, M_ALT, T_ALT, EXPLOSION_AREA);
        assertThat(result).contains("ABC-123");
    }

    @Test
    @DisplayName("formatShortReport — висота цілі з targetAltitude")
    void formatShort_targetAltitude_shown() {
        String result = reportService.formatShortReport(buildReport(), DISTANCE, COURSE, M_ALT, 450, EXPLOSION_AREA);
        assertThat(result).contains("450");
    }

    @Test
    @DisplayName("formatShortReport — містить район підриву")
    void formatShort_containsExplosionArea() {
        String result = reportService.formatShortReport(buildReport(), DISTANCE, COURSE, M_ALT, T_ALT, "Степ");
        assertThat(result).contains("Район підриву: Степ");
    }

    // ========== formatDetailedReport ==========

    @Test
    @DisplayName("formatDetailedReport — пілот БИТКА — містить його ім'я")
    void formatDetailed_pilotBytka_containsName() {
        String result = reportService.formatDetailedReport(buildReport(), "Костянтин БИТКА");
        assertThat(result).contains("Костянтин БИТКА");
        assertThat(result).contains("солдат");
    }

    @Test
    @DisplayName("formatDetailedReport — пілот ШЕПРУК — містить його ім'я")
    void formatDetailed_pilotShepryk_containsName() {
        String result = reportService.formatDetailedReport(buildReport(), "Олександр ШЕПРУК");
        assertThat(result).contains("Олександр ШЕПРУК");
        assertThat(result).contains("старший сержант");
    }

    @Test
    @DisplayName("formatDetailedReport — ціль знищена якщо effectorLossReason містить 'камікадзе'")
    void formatDetailed_kamikazeReason_targetDestroyed() {
        CombatReport report = buildReport();
        report.setEffectorLossReason("Успішне камікадзе ураження");
        String result = reportService.formatDetailedReport(report, "Олександр ШЕПРУК");
        assertThat(result).contains("вражена").doesNotContain("не вражена");
    }

    @Test
    @DisplayName("formatDetailedReport — ціль не вражена якщо причина невідома")
    void formatDetailed_unknownReason_targetNotDestroyed() {
        CombatReport report = buildReport();
        report.setEffectorLossReason("Технічна несправність");
        String result = reportService.formatDetailedReport(report, "Олександр ШЕПРУК");
        assertThat(result).contains("не вражена");
    }

    // ========== extractWeaponName ==========

    @Test
    @DisplayName("weaponId у форматі 'prefix (Назва)' — витягується назва в дужках")
    void weaponId_withParentheses_extractsName() {
        CombatReport report = buildReport();
        report.setWeaponId("drone_v2 (FlyingKamikaze)");
        String result = reportService.formatShortReport(report, DISTANCE, COURSE, M_ALT, T_ALT, EXPLOSION_AREA);
        assertThat(result).contains("FlyingKamikaze");
    }

    @Test
    @DisplayName("weaponId null → використовується AS3 Merops за замовчуванням")
    void weaponId_null_usesDefault() {
        CombatReport report = buildReport();
        report.setWeaponId(null);
        String result = reportService.formatShortReport(report, DISTANCE, COURSE, M_ALT, T_ALT, EXPLOSION_AREA);
        assertThat(result).contains("AS3 Merops");
    }

    // ========== ХЕЛПЕР ==========

    private CombatReport buildReport() {
        CombatReport report = new CombatReport();
        report.setUnitName("СКОПА");
        report.setMilitaryUnit("А0826");
        report.setTargetType("Шахед (Герань)");
        report.setTargetSubType("Шахед (Герань)");
        report.setWeaponId("drone (AS3 Merops)");
        report.setWeaponNumber("ABC123");
        report.setAltitude(300);
        report.setTargetNumberVirazh(5);
        report.setCoordinates("46.4825 N, 30.7233 E");
        report.setEffectorStatus("Засіб витрачено");
        report.setEffectorLossReason("Успішне камікадзе");
        report.setContactTime(ZonedDateTime.parse("2024-05-01T14:30:00+03:00"));
        report.setTakeoffTime(ZonedDateTime.parse("2024-05-01T14:00:00+03:00"));
        return report;
    }
}