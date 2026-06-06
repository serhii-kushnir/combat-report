package org.example.controller;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.example.dto.ConvertRequest;
import org.example.entity.FlightRecord;
import org.example.model.CombatReport;
import org.example.service.FlightRecordService;
import org.example.service.ReportService;
import org.example.validation.ReportRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;
    private final ReportRequestValidator validator;
    private final FlightRecordService flightRecordService;

    public ReportController(ReportService reportService,
                            ReportRequestValidator validator,
                            FlightRecordService flightRecordService) {
        this.reportService = reportService;
        this.validator = validator;
        this.flightRecordService = flightRecordService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

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
                request.getSpeed(),
                request.getCourse(),
                request.getManualAltitude(),
                request.getTargetAltitude()
        );
        if (!errors.isEmpty()) {
            String message = String.join("\n", errors);
            log.warn("Невірний запит: {}", message);
            return badRequest(message);
        }

        try {
            CombatReport report = reportService.parseJson(reportJson);
            int targetAltitude = report.getAltitude() != null ? report.getAltitude() : 0;

            log.info("Конвертація: формат={}, пілот={}, відстань={}м, швидкість={}км/год, курс={}°, висота(ручна)={}м, висота(з JSON)={}м",
                    request.getFormat(), request.getPilot(), request.getDistance(),
                    request.getSpeed(), request.getCourse(), request.getManualAltitude(), targetAltitude);

            // Отримуємо район підриву, якщо null – "Море"
            String explosionArea = request.getExplosionArea() != null ? request.getExplosionArea() : "Море";

            String result = switch (request.getFormat()) {
                case 1 -> reportService.formatStandardReport(report,
                        request.getDistance(),
                        request.getSpeed(),
                        request.getCourse(),
                        request.getManualAltitude(),
                        request.getTargetAltitude());
                case 2 -> reportService.formatShortReport(report,
                        request.getDistance(),
                        request.getCourse(),
                        request.getManualAltitude(),
                        request.getTargetAltitude(),
                        explosionArea);   // ПЕРЕДАЄМО explosionArea
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

    @PostMapping("/save/flight")
    @ResponseBody
    public ResponseEntity<?> saveToJournal(@RequestBody ConvertRequest request) {
        if (request == null || request.getReport() == null) {
            return ResponseEntity.badRequest().body("Дані відсутні");
        }
        try {
            CombatReport report = reportService.parseJson(request.getReport().toString());
            FlightRecord record = mapToFlightRecord(report, request);
            FlightRecord saved = flightRecordService.save(record);
            log.info("Виліт збережено в журнал БпАК: id={}, дата={}", saved.getId(), saved.getFlightDate());
            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "message", "Виліт збережено в журнал БпАК"
            ));
        } catch (Exception e) {
            log.error("Помилка збереження вильоту в журнал", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Помилка: " + e.getMessage()));
        }
    }

    private FlightRecord mapToFlightRecord(CombatReport report, ConvertRequest request) {
        FlightRecord r = new FlightRecord();

        if (report.getContactTime() != null) {
            r.setFlightDate(report.getContactTime().toLocalDate());
            r.setLossTime(report.getContactTime().toLocalTime());
        } else {
            r.setFlightDate(LocalDate.now());
        }
        if (report.getTakeoffTime() != null) {
            r.setTakeoffTime(report.getTakeoffTime().toLocalTime());
        }

        if (r.getFlightDate() != null) {
            String[] UA_MONTHS = {"Січень","Лютий","Березень","Квітень","Травень","Червень",
                    "Липень","Серпень","Вересень","Жовтень","Листопад","Грудень"};
            r.setMonth(UA_MONTHS[r.getFlightDate().getMonthValue() - 1]);
        }

        r.setCrew(report.getUnitName());
        r.setEvent(report.getEffectorStatus());
        r.setCoordinates(report.getCoordinates());
        r.setDistance(request.getDistance());
        r.setAzimuth(request.getCourse());
        r.setTargetType(report.getTargetSubType() != null ? report.getTargetSubType() : report.getTargetType());
        r.setIdentification("Дружній");

        String weaponId = report.getWeaponId();
        if (weaponId != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\(([^)]+)\\)").matcher(weaponId);
            r.setWeapon(m.find() ? m.group(1) : weaponId);
        }
        if (report.getWeaponNumber() != null) {
            r.setWeapon((r.getWeapon() != null ? r.getWeapon() : "") +
                    " (нічний) \"" + report.getWeaponNumber().toUpperCase() + "\"");
        }

        r.setExplosive("ШИФР «3-1.2 КУФ» 1,2 кг");
        r.setDetonator("Вбудована розумна плата ініціації");

        if (request.getManualAltitude() > 0) {
            r.setAltitude(String.valueOf(request.getManualAltitude()));
        } else if (report.getAltitude() != null) {
            r.setAltitude(String.valueOf(report.getAltitude()));
        }

        if (report.getAltitude() != null) {
            r.setTargetAltitude(report.getAltitude());
        }

        String targetNum = report.getTargetNumberVirazh() != null
                ? String.valueOf(report.getTargetNumberVirazh()) : "";
        String targetType = report.getTargetSubType() != null
                ? report.getTargetSubType() : "";
        r.setTarget(targetType + (targetNum.isEmpty() ? "" : " №" + targetNum));
        r.setTargetSpeed(request.getSpeed());
        r.setLossReason(report.getEffectorLossReason());

        String weaponName = r.getWeapon() != null ? r.getWeapon() : "";
        String targetTypeFull = report.getTargetSubType() != null
                ? report.getTargetSubType() : (report.getTargetType() != null ? report.getTargetType() : "");
        String unitNameVal = report.getUnitName() != null ? report.getUnitName() : "";
        String militaryUnitVal = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "";
        String effLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        String generatedNote = "Екіпажем \"" + unitNameVal + "\" в/ч " + militaryUnitVal +
                ", який виконує завдання ведення повітряної розвідки та ураження противника" +
                " в смузі відповідальності ОТУ м. Одеса здійснено виліт дроном-камікадзе" +
                " \"" + weaponName + "\" з метою ураження ворожого ударного дрона №" + targetNum +
                (targetTypeFull.isEmpty() ? "" : " (" + targetTypeFull + ")") + ". " +
                effLossReason;
        r.setNote(generatedNote);

        return r;
    }

    @PostMapping("/save/txt")
    public ResponseEntity<byte[]> saveAsTxt(@RequestParam String report,
                                            @RequestParam String filename) {
        if (report == null || report.isBlank()) {
            return ResponseEntity.badRequest().body("Звіт порожній".getBytes(StandardCharsets.UTF_8));
        }
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
            return ResponseEntity.badRequest().body("Звіт порожній".getBytes(StandardCharsets.UTF_8));
        }
        try {
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
            // ===== НАЛАШТУВАННЯ ПОЛІВ СТОРІНКИ =====
            CTDocument1 ctDocument = document.getDocument();
            CTBody body = ctDocument.getBody();
            if (body.getSectPr() == null) body.addNewSectPr();
            CTSectPr sectPr = body.getSectPr();
            if (sectPr.getPgMar() == null) sectPr.addNewPgMar();
            CTPageMar pageMar = sectPr.getPgMar();

            // Верхнє поле (twips): 567 = 1 см (менше число – підняти верхнє поле)
            pageMar.setTop(567);
            // Нижнє поле (twips): 567 = 1 см (більше число – опустити нижнє поле)
            pageMar.setBottom(567);
            // Ліве і праве – за бажанням (2 см)
//            pageMar.setLeft(1134);
//            pageMar.setRight(1134);
            // =====================================

            String[] lines = text.split("\n");
            for (String line : lines) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setSpacingBefore(0);
                paragraph.setSpacingAfter(0);

                // Ваші умови форматування (залишаються без змін)
                if (line.contains("Командиру екіпажу безпілотних літальних комплексів взводу перехоплювачів безпілотних літальних апаратів військової частини А0826")) {
                    paragraph.setIndentationLeft(5100);
                    paragraph.setAlignment(ParagraphAlignment.LEFT);
                }
                if (line.contains("Командиру взводу перехоплювачів безпілотних літальних апаратів військової частини А0826")) {
                    paragraph.setIndentationLeft(5100);
                    paragraph.setAlignment(ParagraphAlignment.BOTH);
                }
                if (line.contains("Командиру військової частини А0826")) {
                    paragraph.setIndentationLeft(5100);
                    paragraph.setAlignment(ParagraphAlignment.LEFT);
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
                if (line.contains("Оператор безпілотних")
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