package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvertRequest {
    private JsonNode report;
    private int format;
    private String pilot;
    private int distance;
    private int speed;
    private int course;          // Ручний курс (А)
    private int manualAltitude;  // Ручна висота (В)
    private int targetAltitude;  // Висота цілі (з JSON або ручна)
}