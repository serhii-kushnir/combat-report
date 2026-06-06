package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleRowDto {
    private Long id;
    private String shortName;
    private String rank;
    private Map<Integer, String> days; // день -> код статусу (БЧ, В-НЯ, ...)
    // getters, setters
}