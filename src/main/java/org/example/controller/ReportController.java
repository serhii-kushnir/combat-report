package org.example.controller;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.example.model.CombatReport;
import org.example.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class ReportController {

    // [ВИПРАВЛЕННЯ #5] Конструкторна інʼєкція замість @Autowired на полі
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // [ВИПРАВЛЕННЯ #7] Повертає ResponseEntity — JS тепер може відрізнити успіх (200) від помилки (400)
    @PostMapping("/convert")
    @ResponseBody
    public ResponseEntity<String> convertReport(@RequestParam String json,
                                                @RequestParam int format,
                                                @RequestParam String pilot,
                                                @RequestParam int distance,
                                                @RequestParam int speed) {
        try {
            CombatReport report = reportService.parseJson(json);

            String result = switch (format) {
                case 1 -> reportService.formatStandardReport(report, distance, speed);
                case 2 -> reportService.formatShortReport(report);
                case 3 -> reportService.formatDetailedReport(report, pilot);
                default -> throw new IllegalArgumentException("Невідомий формат: " + format);
            };

            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("Помилка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("Помилка парсингу JSON: " + e.getMessage());
        }
    }

    @PostMapping("/save/txt")
    public ResponseEntity<byte[]> saveAsTxt(@RequestParam String report,
                                            @RequestParam String filename) {
        byte[] content = report.getBytes(StandardCharsets.UTF_8);

        // [ВИПРАВЛЕННЯ #3] Прибрано дублювання розширення — JS вже передає filename з .txt
        // [ВИПРАВЛЕННЯ #6] Безпечний Content-Disposition заголовок з лапками та UTF-8 encoded
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
        try {
            byte[] content = createDocxContent(report);

            // [ВИПРАВЛЕННЯ #3] Прибрано дублювання розширення — JS вже передає filename з .docx
            // [ВИПРАВЛЕННЯ #6] Безпечний Content-Disposition заголовок
            String safeFilename = encodeFilename(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + safeFilename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Помилка: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * [ВИПРАВЛЕННЯ #6] Кодує імʼя файлу для безпечного використання в HTTP заголовку.
     * Замінює пробіли та спецсимволи щоб заголовок не зламався.
     */
    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return "report";
        }
    }

    private byte[] createDocxContent(String text) throws Exception {
        // [ВИПРАВЛЕННЯ #7] try-with-resources — XWPFDocument завжди закривається
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