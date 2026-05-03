package org.example.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Централізована валідація вхідних параметрів для /convert.
 * Окремий клас замість if-ів розсипаних по контролеру —
 * легше тестувати і розширювати.
 */
@Component
public class ReportRequestValidator {

    private static final int MIN_FORMAT = 1;
    private static final int MAX_FORMAT = 3;
    private static final int MAX_DISTANCE_M = 100_000;   // 100 км — розумна межа
    private static final int MAX_SPEED_KMH = 1000;       // 1000 км/год — розумна межа для БПЛА
    private static final int MAX_JSON_LENGTH = 50_000;    // 50KB

    private static final Set<String> ALLOWED_PILOTS = Set.of(
            "Костянтин БИТКА",
            "Олександр ШЕПРУК"
    );

    /**
     * Перевіряє всі параметри і повертає список помилок.
     * Порожній список = все валідно.
     */
    public List<String> validate(String json, int format, String pilot, int distance, int speed) {
        List<String> errors = new ArrayList<>();

        // JSON
        if (json == null || json.isBlank()) {
            errors.add("JSON не може бути порожнім");
        } else if (json.length() > MAX_JSON_LENGTH) {
            errors.add("JSON занадто великий: " + json.length() + " символів (максимум " + MAX_JSON_LENGTH + ")");
        } else if (!json.trim().startsWith("{")) {
            errors.add("JSON має починатись з '{' — переданий рядок не схожий на JSON об'єкт");
        }

        // Формат
        if (format < MIN_FORMAT || format > MAX_FORMAT) {
            errors.add("Невідомий формат: " + format + ". Допустимі значення: 1, 2, 3");
        }

        // Пілот
        if (pilot == null || pilot.isBlank()) {
            errors.add("Пілот не вказаний");
        } else if (!ALLOWED_PILOTS.contains(pilot)) {
            errors.add("Невідомий пілот: \"" + pilot + "\". Допустимі: " + ALLOWED_PILOTS);
        }

        // Відстань
        if (distance < 0) {
            errors.add("Відстань не може бути від'ємною: " + distance);
        } else if (distance > MAX_DISTANCE_M) {
            errors.add("Відстань занадто велика: " + distance + " м (максимум " + MAX_DISTANCE_M + " м)");
        }

        // Швидкість
        if (speed < 0) {
            errors.add("Швидкість не може бути від'ємною: " + speed);
        } else if (speed > MAX_SPEED_KMH) {
            errors.add("Швидкість занадто велика: " + speed + " км/год (максимум " + MAX_SPEED_KMH + ")");
        }

        return errors;
    }

    public boolean isValid(String json, int format, String pilot, int distance, int speed) {
        return validate(json, format, pilot, distance, speed).isEmpty();
    }
}