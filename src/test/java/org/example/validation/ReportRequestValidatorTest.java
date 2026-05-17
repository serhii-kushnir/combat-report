package org.example.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportRequestValidator")
class ReportRequestValidatorTest {

    private ReportRequestValidator validator;

    // Валідні дефолтні значення для зручності
    private static final String VALID_JSON = "{\"id\":1}";
    private static final int VALID_FORMAT = 1;
    private static final String VALID_PILOT = "Олександр ШЕПРУК";
    private static final int VALID_DISTANCE = 5000;
    private static final int VALID_SPEED = 160;

    @BeforeEach
    void setUp() {
        validator = new ReportRequestValidator();
    }

    // ========== JSON ==========

    @Test
    @DisplayName("Валідний запит не має помилок")
    void validRequest_noErrors() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("JSON null → помилка")
    void nullJson_hasError() {
        List<String> errors = validator.validate(null, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("порожнім"));
    }

    @Test
    @DisplayName("JSON порожній рядок → помилка")
    void blankJson_hasError() {
        List<String> errors = validator.validate("   ", VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("порожнім"));
    }

    @Test
    @DisplayName("JSON не починається з '{' → помилка")
    void jsonNotObject_hasError() {
        List<String> errors = validator.validate("[1,2,3]", VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("починатись з '{'"));
    }

    @Test
    @DisplayName("JSON більше 50KB → помилка")
    void jsonTooLarge_hasError() {
        String bigJson = "{\"x\":\"" + "a".repeat(50_000) + "\"}";
        List<String> errors = validator.validate(bigJson, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("занадто великий"));
    }

    // ========== ФОРМАТ ==========

    @ParameterizedTest(name = "Формат {0} → валідний")
    @ValueSource(ints = {1, 2, 3})
    @DisplayName("Допустимі формати 1-3 не дають помилок")
    void validFormats_noError(int format) {
        List<String> errors = validator.validate(VALID_JSON, format, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).noneMatch(e -> e.contains("формат"));
    }

    @ParameterizedTest(name = "Формат {0} → помилка")
    @ValueSource(ints = {0, 4, -1, 99})
    @DisplayName("Недопустимі формати дають помилку")
    void invalidFormats_hasError(int format) {
        List<String> errors = validator.validate(VALID_JSON, format, VALID_PILOT, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("формат"));
    }

    // ========== ПІЛОТ ==========

    @ParameterizedTest(name = "Пілот \"{0}\" → валідний")
    @ValueSource(strings = {"Костянтин БИТКА", "Олександр ШЕПРУК"})
    @DisplayName("Дозволені пілоти не дають помилок")
    void allowedPilots_noError(String pilot) {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, pilot, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).noneMatch(e -> e.toLowerCase().contains("пілот"));
    }

    @Test
    @DisplayName("Невідомий пілот → помилка")
    void unknownPilot_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, "Невідомий ПІЛОТ", VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("Невідомий пілот"));
    }

    @Test
    @DisplayName("Пілот null → помилка")
    void nullPilot_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, null, VALID_DISTANCE, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("Пілот не вказаний"));
    }

    // ========== ВІДСТАНЬ ==========

    @Test
    @DisplayName("Відстань від'ємна → помилка")
    void negativeDistance_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, -1, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("від'ємною"));
    }

    @Test
    @DisplayName("Відстань 0 → валідна")
    void zeroDistance_noError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, 0, VALID_SPEED);
        assertThat(errors).noneMatch(e -> e.contains("Відстань"));
    }

    @Test
    @DisplayName("Відстань понад 100км → помилка")
    void tooLargeDistance_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, 100_001, VALID_SPEED);
        assertThat(errors).anyMatch(e -> e.contains("занадто велика"));
    }

    // ========== ШВИДКІСТЬ ==========

    @Test
    @DisplayName("Швидкість від'ємна → помилка")
    void negativeSpeed_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, -1);
        assertThat(errors).anyMatch(e -> e.contains("від'ємною"));
    }

    @Test
    @DisplayName("Швидкість понад 1000 км/год → помилка")
    void tooHighSpeed_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, 1001);
        assertThat(errors).anyMatch(e -> e.contains("занадто велика"));
    }

    // ========== КІЛЬКА ПОМИЛОК ОДНОЧАСНО ==========

    @Test
    @DisplayName("Кілька невалідних полів → всі помилки повертаються")
    void multipleErrors_allReturned() {
        List<String> errors = validator.validate(null, 99, null, -1, -1);
        assertThat(errors).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("isValid повертає true для валідного запиту")
    void isValid_validRequest_returnsTrue() {
        assertThat(validator.isValid(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED)).isTrue();
    }

    @Test
    @DisplayName("isValid повертає false для невалідного запиту")
    void isValid_invalidRequest_returnsFalse() {
        assertThat(validator.isValid(null, 0, null, -1, -1)).isFalse();
    }
    // ========== КУРС ==========

    @Test
    @DisplayName("Курс від'ємний → помилка")
    void negativeCourse_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED, -1, 0, 0);
        assertThat(errors).anyMatch(e -> e.contains("Курс"));
    }

    @Test
    @DisplayName("Курс більше 360° → помилка")
    void tooLargeCourse_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED, 361, 0, 0);
        assertThat(errors).anyMatch(e -> e.contains("Курс"));
    }

    @Test
    @DisplayName("Курс 0-360 → валідний")
    void validCourse_noError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED, 270, 0, 0);
        assertThat(errors).noneMatch(e -> e.contains("Курс"));
    }

    // ========== ВИСОТА ==========

    @Test
    @DisplayName("Висота від'ємна → помилка")
    void negativeAltitude_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED, 0, -1, 0);
        assertThat(errors).anyMatch(e -> e.contains("Висота"));
    }

    @Test
    @DisplayName("Висота цілі від'ємна → помилка")
    void negativeTargetAltitude_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED, 0, 0, -1);
        assertThat(errors).anyMatch(e -> e.contains("Висота цілі"));
    }

    @Test
    @DisplayName("Висота понад 20км → помилка")
    void tooHighAltitude_hasError() {
        List<String> errors = validator.validate(VALID_JSON, VALID_FORMAT, VALID_PILOT, VALID_DISTANCE, VALID_SPEED, 0, 20_001, 0);
        assertThat(errors).anyMatch(e -> e.contains("занадто велика"));
    }

}
