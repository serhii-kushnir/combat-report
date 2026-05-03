package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * DTO для запиту конвертації.
 * Замість form-параметрів — один JSON об'єкт у тілі запиту.
 *
 * Було (form-urlencoded):
 *   json=...&format=1&pilot=...&distance=500&speed=160
 *
 * Стало (application/json):
 *   {
 *     "report": { ...бойовий звіт... },
 *     "format": 1,
 *     "pilot": "Олександр ШЕПРУК",
 *     "distance": 500,
 *     "speed": 160
 *   }
 *
 * Переваги:
 * - Немає обмежень розміру form-параметра
 * - Типізований — Jackson валідує структуру автоматично
 * - Стандартний REST підхід
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvertRequest {

    /**
     * Об'єкт бойового звіту — передається напряму як JSON вузол,
     * а не як рядок. ReportService десеріалізує його в CombatReport.
     */
    private JsonNode report;

    /** Формат звіту: 1 = стандартний, 2 = скорочений, 3 = детальний */
    private int format;

    /** Ім'я пілота */
    private String pilot;

    /** Відстань від місця вильоту в метрах */
    private int distance;

    /** Швидкість цілі в км/год */
    private int speed;
}