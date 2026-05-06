package org.example.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ReportRequestValidator {

    private static final int MIN_FORMAT = 1;
    private static final int MAX_FORMAT = 3;
    private static final int MAX_DISTANCE_M = 100_000;
    private static final int MAX_SPEED_KMH = 1000;
    private static final int MAX_JSON_LENGTH = 50_000;
    private static final int MAX_COURSE_DEG = 360;
    private static final int MAX_ALTITUDE_M = 20_000;   // 20км — розумна межа для БПЛА

    private static final Set<String> ALLOWED_PILOTS = Set.of(
            "Костянтин БИТКА",
            "Олександр ШЕПРУК"
    );

    public List<String> validate(String json, int format, String pilot, int distance, int speed) {
        return validate(json, format, pilot, distance, speed, 0, 0, 0);
    }

    /**
     * [ВИПРАВЛЕННЯ #4] Розширена валідація — тепер перевіряє course, manualAltitude, targetAltitude
     */
    public List<String> validate(String json, int format, String pilot,
                                 int distance, int speed,
                                 int course, int manualAltitude, int targetAltitude) {
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

        // Курс
        if (course < 0 || course > MAX_COURSE_DEG) {
            errors.add("Курс має бути від 0 до 360°: " + course);
        }

        // Висота (ручна)
        if (manualAltitude < 0) {
            errors.add("Висота не може бути від'ємною: " + manualAltitude);
        } else if (manualAltitude > MAX_ALTITUDE_M) {
            errors.add("Висота занадто велика: " + manualAltitude + " м (максимум " + MAX_ALTITUDE_M + " м)");
        }

        // Висота цілі
        if (targetAltitude < 0) {
            errors.add("Висота цілі не може бути від'ємною: " + targetAltitude);
        } else if (targetAltitude > MAX_ALTITUDE_M) {
            errors.add("Висота цілі занадто велика: " + targetAltitude + " м (максимум " + MAX_ALTITUDE_M + " м)");
        }

        return errors;
    }

    public boolean isValid(String json, int format, String pilot, int distance, int speed) {
        return validate(json, format, pilot, distance, speed).isEmpty();
    }
}
