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
    private static final int MAX_ALTITUDE_M = 20_000;

    private static final Set<String> ALLOWED_PILOTS = Set.of(
            "Костянтин БИТКА",
            "Олександр ШЕПРУК",
            "Ярослав НАГОРНИЙ"
    );

    public List<String> validate(String json, int format, String pilot, int distance, int speed) {
        return validate(json, format, pilot, distance, speed, 0, 0, 0, 0);
    }

    public List<String> validate(String json, int format, String pilot,
                                 int distance, int speed,
                                 int course, int manualAltitude, int targetAltitude) {
        return validate(json, format, pilot, distance, speed, course, manualAltitude, targetAltitude, 0);
    }

    // НОВИЙ МЕТОД З ПАРАМЕТРОМ courseDirection
    public List<String> validate(String json, int format, String pilot,
                                 int distance, int speed,
                                 int course, int manualAltitude, int targetAltitude,
                                 int courseDirection) {
        List<String> errors = new ArrayList<>();

        // ... перевірки JSON, формату, пілота, відстані, швидкості ...
        // (ті самі, що були)

        // Курс (азимут)
        if (course < 0 || course > MAX_COURSE_DEG) {
            errors.add("Азимут має бути від 0 до 360°: " + course);
        }

        // НОВА ПЕРЕВІРКА: Курс (напрямок)
        if (courseDirection < 0 || courseDirection > MAX_COURSE_DEG) {
            errors.add("Курс має бути від 0 до 360°: " + courseDirection);
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
}