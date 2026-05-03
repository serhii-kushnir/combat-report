package org.example.controller;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.example.dto.ConvertRequest;
import org.example.model.CombatReport;
import org.example.service.ReportService;
import org.example.validation.ReportRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;
    private final ReportRequestValidator validator;

    public ReportController(ReportService reportService, ReportRequestValidator validator) {
        this.reportService = reportService;
        this.validator = validator;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Рефакторинг: було @RequestParam (form-urlencoded) — стало @RequestBody (application/json).
     * JS тепер надсилає один JSON об'єкт { report: {...}, format, pilot, distance, speed }.
     * report передається як JsonNode — без подвійної серіалізації через рядок.
     */
    @PostMapping("/convert")
    @ResponseBody
    public ResponseEntity<String> convertReport(@RequestBody ConvertRequest request) {
        if (request == null || request.getReport() == null) {
            return badRequest("Тіло запиту не може бути порожнім");
        }

        String reportJson = request.getReport().toString();

        List<String> errors = validator.validate(
                reportJson,
                request.getFormat(),
                request.getPilot(),
                request.getDistance(),
                request.getSpeed()
        );
        if (!errors.isEmpty()) {
            String message = String.join("\n", errors);
            log.warn("Невірний запит: {}", message);
            return badRequest(message);
        }

        try {
            CombatReport report = reportService.parseJson(reportJson);
            log.info("Конвертація: формат={}, пілот={}, відстань={}м, швидкість={}км/год",
                    request.getFormat(), request.getPilot(), request.getDistance(), request.getSpeed());

            String result = switch (request.getFormat()) {
                case 1 -> reportService.formatStandardReport(report, request.getDistance(), request.getSpeed());
                case 2 -> reportService.formatShortReport(report);
                case 3 -> reportService.formatDetailedReport(report, request.getPilot());
                default -> throw new IllegalArgumentException("Невідомий формат: " + request.getFormat());
            };

            log.info("Звіт сформовано: {} рядків", result.split("\n").length);
            return ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Невірний аргумент: {}", e.getMessage());
            return badRequest("Помилка: " + e.getMessage());
        } catch (Exception e) {
            log.error("Помилка обробки звіту: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("Помилка обробки звіту: " + e.getMessage());
        }
    }

    @PostMapping("/save/txt")
    public ResponseEntity<byte[]> saveAsTxt(@RequestParam String report,
                                            @RequestParam String filename) {
        if (report == null || report.isBlank()) {
            log.warn("Спроба зберегти порожній TXT звіт");
            return ResponseEntity.badRequest().body("Звіт порожній".getBytes(StandardCharsets.UTF_8));
        }

        log.info("Збереження TXT: {}", filename);
        byte[] content = report.getBytes(StandardCharsets.UTF_8);
        String safeFilename = encodeFilename(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + safeFilename)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(content);
    }

    @PostMapping("/save/docx")
    public ResponseEntity<byte[]> saveAsDocx(@RequestParam String report,
                                             @RequestParam String filename) {
        if (report == null || report.isBlank()) {
            log.warn("Спроба зберегти порожній DOCX звіт");
            return ResponseEntity.badRequest().body("Звіт порожній".getBytes(StandardCharsets.UTF_8));
        }

        try {
            log.info("Збереження DOCX: {}", filename);
            byte[] content = createDocxContent(report);
            String safeFilename = encodeFilename(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + safeFilename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            log.error("Помилка створення DOCX: {}", filename, e);
            return ResponseEntity.internalServerError()
                    .body(("Помилка створення DOCX: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    // ========== ХЕЛПЕРИ ==========

    private ResponseEntity<String> ok(String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(body);
    }

    private ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.badRequest()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(message);
    }

    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            log.warn("Не вдалось закодувати ім'я файлу: {}", filename);
            return "report";
        }
    }

    private byte[] createDocxContent(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
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
                if (line.contains("Командир взводу перехоплювачів")
                        && !line.contains("Клопочу")
                        && !line.contains("військової частини")
                        && !line.contains("Командиру")) {
                    paragraph.setAlignment(ParagraphAlignment.RIGHT);
                }
                if (line.trim().equals("Рапорт") && !line.contains("Клопочу")) {
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                }
                if (line.contains("Клопочу по суті")) {
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                }
                if (line.contains("Пілот:") || line.contains("Оператор безпілотних")
                        || line.contains("взводу перехоплювачів безпілотних")
                        || line.contains("\tДійсним доповідаю")
                        || line.contains("Командир екіпажу:") || line.contains("Командир екіпажу безпілотних")
                        || (line.contains("Командир взводу перехоплювачів")
                        && line.contains("військової частини")
                        && !line.contains("Командиру"))) {
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
            return out.toByteArray();
        }
    }
}