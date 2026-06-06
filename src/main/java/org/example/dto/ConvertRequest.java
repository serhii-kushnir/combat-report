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
    private int course;
    private int manualAltitude;
    private int targetAltitude;
    private String explosionArea;   // НОВЕ ПОЛЕ – Район підриву
}