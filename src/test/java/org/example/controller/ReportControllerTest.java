package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ReportController — інтеграційні тести")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Map<String, Object> VALID_REPORT = Map.of(
            "unitName", "СКОПА",
            "militaryUnit", "А0826",
            "targetType", "Шахед (Герань)",
            "weaponId", "drone (AS3 Merops)",
            "weaponNumber", "ABC123",
            "altitude", 300,
            "targetNumberVirazh", 5,
            "effectorStatus", "Засіб витрачено",
            "effectorLossReason", "Успішне камікадзе"
    );

    @Test
    @DisplayName("POST /convert — формат 1 повертає 200 і текст звіту")
    void convert_format1_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 1,
                "pilot", "Олександр ШЕПРУК",
                "distance", 5000,
                "speed", 160,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 300
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("СКОПА")))
                .andExpect(content().string(containsString("5000 м")));
    }

    @Test
    @DisplayName("POST /convert — формат 2 повертає скорочений звіт")
    void convert_format2_returnsShortReport() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 2,
                "pilot", "Олександр ШЕПРУК",
                "distance", 5000,
                "speed", 160,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 300
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Екіпаж:")))
                .andExpect(content().string(containsString("Номер цілі по Віражу:"))); // ЗМІНЕНО
    }

    @Test
    @DisplayName("POST /convert — формат 3 повертає детальний звіт з рапортом")
    void convert_format3_returnsDetailedReport() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 3,
                "pilot", "Костянтин БИТКА",
                "distance", 5000,
                "speed", 160,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 300
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Рапорт")))
                .andExpect(content().string(containsString("Костянтин БИТКА")));
    }

    @Test
    @DisplayName("POST /convert — невалідний формат повертає 400")
    void convert_invalidFormat_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 99,
                "pilot", "Олександр ШЕПРУК",
                "distance", 5000,
                "speed", 160,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 0
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("формат")));
    }

    @Test
    @DisplayName("POST /convert — невідомий пілот повертає 400")
    void convert_unknownPilot_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 1,
                "pilot", "Невідомий ПІЛОТ",
                "distance", 5000,
                "speed", 160,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 0
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Невідомий пілот")));
    }

    @Test
    @DisplayName("POST /convert — від'ємна відстань повертає 400")
    void convert_negativeDistance_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 1,
                "pilot", "Олександр ШЕПРУК",
                "distance", -1,
                "speed", 160,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 0
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("від'ємною")));
    }

    @Test
    @DisplayName("POST /convert — порожнє тіло запиту повертає 400")
    void convert_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /convert — кілька помилок валідації повертаються разом")
    void convert_multipleErrors_allReturned() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "report", VALID_REPORT,
                "format", 99,
                "pilot", "Невідомий ПІЛОТ",
                "distance", -1,
                "speed", -1,
                "course", 0,
                "manualAltitude", 0,
                "targetAltitude", 0
        ));

        mockMvc.perform(post("/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("формат")))
                .andExpect(content().string(containsString("пілот")));
    }

    @Test
    @DisplayName("POST /save/txt — повертає файл з правильним заголовком")
    void saveTxt_returnsFile() throws Exception {
        mockMvc.perform(post("/save/txt")
                        .param("report", "Тестовий звіт\nРядок 2")
                        .param("filename", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("Тестовий звіт")));
    }

    @Test
    @DisplayName("POST /save/txt — порожній звіт повертає 400")
    void saveTxt_emptyReport_returns400() throws Exception {
        mockMvc.perform(post("/save/txt")
                        .param("report", "   ")
                        .param("filename", "test.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /save/docx — повертає файл")
    void saveDocx_returnsFile() throws Exception {
        mockMvc.perform(post("/save/docx")
                        .param("report", "Тестовий звіт")
                        .param("filename", "test.docx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    @DisplayName("POST /save/docx — порожній звіт повертає 400")
    void saveDocx_emptyReport_returns400() throws Exception {
        mockMvc.perform(post("/save/docx")
                        .param("report", "")
                        .param("filename", "test.docx"))
                .andExpect(status().isBadRequest());
    }
}