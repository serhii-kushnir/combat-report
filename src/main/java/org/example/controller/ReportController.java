package org.example.controller;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.example.model.CombatReport;
import org.example.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

@Controller
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/convert")
    @ResponseBody
    public String convertReport(@RequestParam String json,
                                @RequestParam int format,
                                @RequestParam String pilot,
                                @RequestParam int distance,
                                @RequestParam int speed) {
        try {
            CombatReport report = reportService.parseJson(json);

            switch (format) {
                case 1:
                    return reportService.formatStandardReport(report, distance, speed);
                case 2:
                    return reportService.formatShortReport(report);
                case 3:
                    return reportService.formatDetailedReport(report, pilot);
                default:
                    return "Невідомий формат";
            }
        } catch (Exception e) {
            return "Помилка: " + e.getMessage();
        }
    }

    @PostMapping("/save/txt")
    public ResponseEntity<byte[]> saveAsTxt(@RequestParam String report,
                                            @RequestParam String filename) {
        byte[] content = report.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @PostMapping("/save/docx")
    public ResponseEntity<byte[]> saveAsDocx(@RequestParam String report,
                                             @RequestParam String filename) {
        try {
            byte[] content = createDocxContent(report);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".docx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(("Помилка: " + e.getMessage()).getBytes());
        }
    }

    private byte[] createDocxContent(String text) throws Exception {
        XWPFDocument document = new XWPFDocument();
        String[] lines = text.split("\n");

        for (String line : lines) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setSpacingBefore(0);
            paragraph.setSpacingAfter(0);

            if (line.contains("Командиру взводу перехоплювачів")) {
                paragraph.setIndentationLeft(5400);
                paragraph.setAlignment(ParagraphAlignment.LEFT);
            }

            if (line.contains("Начальнику позаштатної служби безпілотних авіаційних комплексів")) {
                paragraph.setIndentationLeft(5400);
                paragraph.setAlignment(ParagraphAlignment.BOTH);
            }

            if (line.contains("Командир взводу перехоплювачів") && !line.contains("Клопочу") && !line.contains("військової частини") && !line.contains("Командиру")) {
                paragraph.setAlignment(ParagraphAlignment.RIGHT);
            }

            if (line.trim().equals("Рапорт") && !line.contains("Клопочу")) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
            }

            if (line.contains("Клопочу по суті")) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
            }

            if (line.contains("Пілот:") || line.contains("Оператор безпілотних") ||
                    line.contains("взводу перехоплювачів безпілотних") ||
                    line.contains("\tДійсним доповідаю") ||
                    line.contains("Командир екіпажу:") || line.contains("Командир екіпажу безпілотних") ||
                    line.contains("Командир взводу перехоплювачів") && line.contains("військової частини") && !line.contains("Командиру")) {
                paragraph.setAlignment(ParagraphAlignment.BOTH);
            }

            XWPFRun run = paragraph.createRun();
            run.setFontFamily("Times New Roman");
            run.setFontSize(12);
            run.setLang("uk-UA");
            run.setText(line);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();
        return out.toByteArray();
    }
}