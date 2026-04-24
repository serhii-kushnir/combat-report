package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.xwpf.usermodel.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CombatReportGUI extends JFrame {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JLabel statusLabel;
    private JLabel fileLabel;
    private File selectedFile;
    private JComboBox<String> formatCombo;
    private JComboBox<String> pilotCombo;  // Комбобокс для вибору пілота

    private JTextField distanceField;
    private JTextField speedField;

    public CombatReportGUI() {
        setTitle("Конвертер бойових звітів JSON -> TXT / PDF / DOCX");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 850);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void setNumericOnly(JTextField textField) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                if (string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) return;
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = createSplitPane();
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEtchedBorder());

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Параметри звіту"));

        JLabel formatLabel = new JLabel("Формат звіту:");
        formatLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(formatLabel);

        String[] formats = {"[1] Стандартний формат", "[2] Скорочений формат (Екіпаж/Ціль)", "[3] Детальний формат (Дійсним доповідаю)"};
        formatCombo = new JComboBox<>(formats);
        formatCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formatCombo.setPreferredSize(new Dimension(320, 28));
        inputPanel.add(formatCombo);

        // ========== ВИБІР ПІЛОТА ==========
        JLabel pilotLabel = new JLabel("Пілот:");
        pilotLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(pilotLabel);

        String[] pilots = {"Костянтин БИТКА", "Ярослав НАГОРНИЙ"};
        pilotCombo = new JComboBox<>(pilots);
        pilotCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pilotCombo.setPreferredSize(new Dimension(180, 28));
        inputPanel.add(pilotCombo);
        // ================================

        JLabel distanceLabel = new JLabel("Відстань від місця взльоту (м):");
        distanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(distanceLabel);

        distanceField = new JTextField("10000", 8);
        distanceField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        distanceField.setPreferredSize(new Dimension(100, 28));
        distanceField.setToolTipText("Відстань від місця взльоту в метрах (тільки цифри)");
        setNumericOnly(distanceField);
        inputPanel.add(distanceField);

        JLabel speedLabel = new JLabel("Швидкість цілі (км/год):");
        speedLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(speedLabel);

        speedField = new JTextField("160", 8);
        speedField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        speedField.setPreferredSize(new Dimension(80, 28));
        speedField.setToolTipText("Швидкість цілі в км/год (тільки цифри)");
        setNumericOnly(speedField);
        inputPanel.add(speedField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Дії"));

        JButton fileButton = new JButton("Вибрати JSON файл");
        fileButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fileButton.setBackground(new Color(59, 89, 182));
        fileButton.setForeground(Color.BLACK);
        fileButton.setFocusPainted(false);
        fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileButton.addActionListener(e -> chooseJsonFile());
        buttonPanel.add(fileButton);

        JButton convertButton = new JButton("Конвертувати");
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        convertButton.setBackground(new Color(0, 128, 0));
        convertButton.setForeground(Color.BLACK);
        convertButton.setFocusPainted(false);
        convertButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        convertButton.addActionListener(e -> convertToReport());
        buttonPanel.add(convertButton);

        JButton copyButton = new JButton("Копіювати");
        copyButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        copyButton.setBackground(new Color(70, 130, 180));
        copyButton.setForeground(Color.BLACK);
        copyButton.setFocusPainted(false);
        copyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyButton.addActionListener(e -> copyToClipboard());
        buttonPanel.add(copyButton);

        JButton saveTxtButton = new JButton("Зберегти TXT");
        saveTxtButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveTxtButton.setBackground(new Color(70, 130, 180));
        saveTxtButton.setForeground(Color.BLACK);
        saveTxtButton.setFocusPainted(false);
        saveTxtButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveTxtButton.addActionListener(e -> saveReportAsTxt());
        buttonPanel.add(saveTxtButton);

        JButton savePdfButton = new JButton("Зберегти PDF");
        savePdfButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        savePdfButton.setBackground(new Color(178, 34, 34));
        savePdfButton.setForeground(Color.BLACK);
        savePdfButton.setFocusPainted(false);
        savePdfButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        savePdfButton.addActionListener(e -> saveReportAsPdf());
        buttonPanel.add(savePdfButton);

        JButton saveDocxButton = new JButton("Зберегти DOCX");
        saveDocxButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveDocxButton.setBackground(new Color(46, 125, 50));
        saveDocxButton.setForeground(Color.BLACK);
        saveDocxButton.setFocusPainted(false);
        saveDocxButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveDocxButton.addActionListener(e -> saveReportAsDocx());
        buttonPanel.add(saveDocxButton);

        JButton clearButton = new JButton("Очистити");
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearButton.setBackground(new Color(220, 20, 60));
        clearButton.setForeground(Color.BLACK);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearAll());
        buttonPanel.add(clearButton);

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileLabel = new JLabel("[Файл] Файл не вибрано");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileLabel.setForeground(new Color(100, 100, 100));
        filePanel.add(fileLabel);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(inputPanel, BorderLayout.NORTH);
        topContainer.add(buttonPanel, BorderLayout.CENTER);
        topContainer.add(filePanel, BorderLayout.SOUTH);

        panel.add(topContainer, BorderLayout.CENTER);

        return panel;
    }

    private JSplitPane createSplitPane() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("[JSON] Вхідний JSON"));

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        inputArea.setBackground(new Color(250, 250, 250));
        inputArea.setForeground(Color.BLACK);
        inputArea.setLineWrap(true);
        JScrollPane leftScroll = new JScrollPane(inputArea);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        leftPanel.add(leftScroll, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("[TXT] Форматований звіт"));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setBackground(new Color(255, 255, 240));
        outputArea.setForeground(Color.BLACK);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        JScrollPane rightScroll = new JScrollPane(outputArea);
        rightScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rightPanel.add(rightScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);

        return splitPane;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setPreferredSize(new Dimension(panel.getWidth(), 60));

        statusLabel = new JLabel("[OK] Готовий до роботи. Виберіть JSON файл або вставте JSON вручну.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(Color.BLACK);
        panel.add(statusLabel, BorderLayout.WEST);

        JLabel authorLabel = new JLabel("by Serhii Kushnir");
        authorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        authorLabel.setForeground(new Color(100, 100, 100));
        panel.add(authorLabel, BorderLayout.EAST);

        return panel;
    }

    private void chooseJsonFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Виберіть JSON файл");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON файли (*.json)", "json"));

        if (selectedFile != null) {
            fileChooser.setCurrentDirectory(selectedFile.getParentFile());
        } else {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            fileLabel.setText("[Файл] " + selectedFile.getAbsolutePath());
            statusLabel.setText("[OK] Файл вибрано: " + selectedFile.getName());
            loadJsonFromFile(selectedFile);
        }
    }

    private void loadJsonFromFile(File file) {
        try {
            String jsonContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            inputArea.setText(jsonContent);
            statusLabel.setText("[OK] JSON завантажено з файлу: " + file.getName());
        } catch (IOException e) {
            statusLabel.setText("[X] Помилка читання файлу: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Не вдалося прочитати файл:\n" + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void convertToReport() {
        String jsonText = inputArea.getText().trim();

        if (jsonText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Будь ласка, введіть JSON або виберіть файл.",
                    "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            CombatReport report = objectMapper.readValue(jsonText, CombatReport.class);

            String formattedReport;
            int selectedIndex = formatCombo.getSelectedIndex();
            if (selectedIndex == 0) {
                formattedReport = formatStandardReport(report);
            } else if (selectedIndex == 1) {
                formattedReport = formatShortReport(report);
            } else {
                formattedReport = formatDetailedReport(report);
            }

            outputArea.setText(formattedReport);
            statusLabel.setText("[OK] Конвертація виконана успішно!");

            int lines = formattedReport.split("\n").length;
            statusLabel.setText(statusLabel.getText() + " (" + lines + " рядків)");

        } catch (Exception e) {
            statusLabel.setText("[X] Помилка конвертації: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Помилка парсингу JSON:\n" + e.getMessage() +
                            "\n\nПеревірте формат JSON файлу.",
                    "Помилка конвертації", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatStandardReport(CombatReport report) {
        StringBuilder sb = new StringBuilder();

        int manualDistance;
        int manualSpeed;

        try {
            manualDistance = Integer.parseInt(distanceField.getText().trim());
        } catch (NumberFormatException e) {
            manualDistance = 10000;
        }

        try {
            manualSpeed = Integer.parseInt(speedField.getText().trim());
        } catch (NumberFormatException e) {
            manualSpeed = 160;
        }

        String takeoffTime = "";
        String lossTime = "";
        String reportDate = "";

        if (report.getTakeoffTime() != null) {
            takeoffTime = report.getTakeoffTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER);
        }

        if (report.getContactTime() != null) {
            reportDate = report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(DATE_FORMATTER);
            lossTime = report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER);
        } else {
            reportDate = java.time.LocalDate.now().format(DATE_FORMATTER);
            lossTime = java.time.LocalTime.now().format(TIME_FORMATTER);
        }

        if (!reportDate.isEmpty()) {
            sb.append(reportDate).append("\n");
        }

        sb.append("Екіпаж: ").append(report.getUnitName()).append("\n");

        String effectorStatus = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        sb.append(effectorStatus).append("\n");

        sb.append("\n");

        sb.append("Час: ").append(takeoffTime).append(" - ").append(lossTime).append("\n");

        String coordinates = convertMgrsToLatLon(report.getCoordinates());
        sb.append("Координати: ").append(coordinates).append("\n");

        sb.append("Відстань від місця взльоту: ").append(manualDistance).append(" м\n");

        sb.append("Тип: ").append(report.getTargetType()).append("\n");

        sb.append("Ідентифікація: Дружній\n");

        String weapon = report.getWeaponId() != null ?
                report.getWeaponId().replace(" (AS3 Merops)", "") : "Merops";
        sb.append("Засіб ураження: ").append(weapon)
                .append(" ").append(report.getWeaponNumber()).append("\n");

        sb.append("Вибухівка: ШИФР «3-1.2 КУФ» 1.2 кг (8g 35m H)\n");
        sb.append("Детонатор: Вбудована розумна плата ініціації.\n");

        String unitName = report.getUnitName() != null ? report.getUnitName() : "";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "";
        String geoMarker = report.getGeoMarker() != null ? report.getGeoMarker() : "";
        String targetNumber = report.getTargetNumberVirazh() != 0 ? String.valueOf(report.getTargetNumberVirazh()) : "";
        String effectorStatusForNote = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        sb.append("Примітка: Екіпажем ").append(unitName)
                .append(", ").append(militaryUnit)
                .append(", який виконує завдання ведення повітряної розвідки та ураження противника в смузі відповідальності ОТУ ").append(geoMarker)
                .append(", дроном - камікадзе (Merops) було здійснено виліт з метою ураження ворожого ударного дрона ").append(targetNumber)
                .append(", ").append(effectorStatusForNote)
                .append(", ").append(effectorLossReason).append("\n");

        int altitude = report.getAltitude() != 0 ? report.getAltitude() : 500;
        String targetSubType = report.getTargetSubType() != null ? report.getTargetSubType() : "Інший";
        int targetNum = report.getTargetNumberVirazh() != 0 ? report.getTargetNumberVirazh() : 0;
        String targetSkymap = report.getTargetNumberSkymap() != null && !report.getTargetNumberSkymap().isEmpty()
                ? report.getTargetNumberSkymap() : null;
        int targetSpeed = manualSpeed;

        sb.append("Висота: ").append(altitude).append("м, ціль ").append(targetSubType);

        if (targetNum != 0) {
            sb.append(" ").append(targetNum).append(" (по віражу)");
        }

        if (targetSkymap != null && !targetSkymap.isEmpty()) {
            sb.append(", ").append(targetSkymap).append(" (по скаймапі)");
        }

        sb.append(", швидкість цілі: ").append(targetSpeed).append(" км/год.");

        return sb.toString();
    }

    private String formatShortReport(CombatReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("Екіпаж: ").append(report.getUnitName()).append("\n");

        if (report.getTakeoffTime() != null) {
            sb.append("Час вильоту: ").append(report.getTakeoffTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER)).append("\n");
        }

        if (report.getContactTime() != null) {
            sb.append("Час підриву по цілі: ").append(report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER)).append("\n");
        }

        String region = report.getGeoMarker() != null && !report.getGeoMarker().isEmpty()
                ? report.getGeoMarker() : "Невідомо";
        sb.append("Район підриву по цілі: ").append(region).append("\n");

        String coordinates = convertMgrsToLatLon(report.getCoordinates());
        sb.append("Приблизні координати підриву по цілі: ").append(coordinates).append("\n");

        String targetType = report.getTargetSubType() != null
                ? report.getTargetSubType() : report.getTargetType();
        sb.append("Тип цілі: ").append(targetType).append("\n");

        sb.append("Висота цілі: ").append(report.getAltitude()).append(" метрів\n");

        String weapon = report.getWeaponId() != null
                ? report.getWeaponId().replace(" (AS3 Merops)", "") : "Merops";
        sb.append("Засіб ураження: ").append(weapon)
                .append(" ").append(report.getWeaponNumber()).append("\n");

        sb.append("Номер цілі по Віражу: ").append(report.getTargetNumberVirazh()).append("\n");

        String skymap = report.getTargetNumberSkymap() != null && !report.getTargetNumberSkymap().isEmpty()
                ? report.getTargetNumberSkymap() : String.valueOf(report.getTargetNumberVirazh());
        sb.append("Номер по СкайМаті: ").append(skymap).append("\n");

        return sb.toString();
    }

    private String formatDetailedReport(CombatReport report) {
        StringBuilder sb = new StringBuilder();

        String unitName = report.getUnitName() != null ? report.getUnitName() : "";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "";
        String geoMarker = report.getGeoMarker() != null ? report.getGeoMarker() : "Одеса";
        String targetNumber = report.getTargetNumberVirazh() != 0 ? String.valueOf(report.getTargetNumberVirazh()) : "";
        String targetSubType = report.getTargetSubType() != null ? report.getTargetSubType() : "Шахед (Герань)";
        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        boolean targetDestroyed = effectorLossReason.toLowerCase().contains("успішне") ||
                effectorLossReason.toLowerCase().contains("вражена") ||
                effectorLossReason.toLowerCase().contains("знищ") ||
                effectorLossReason.toLowerCase().contains("камікадзе");

        String reportDate;
        String takeoffTime = "";
        String contactTime = "";

        if (report.getContactTime() != null) {
            reportDate = report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(DATE_FORMATTER);
            contactTime = report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER);
        } else {
            reportDate = java.time.LocalDate.now().format(DATE_FORMATTER);
            contactTime = java.time.LocalTime.now().format(TIME_FORMATTER);
        }

        if (report.getTakeoffTime() != null) {
            takeoffTime = report.getTakeoffTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER);
        } else {
            takeoffTime = contactTime;
        }

        String regionName = geoMarker.equals("Одеса") ? "Одеської" : geoMarker + "ської";
        String targetResult = targetDestroyed ? "вражена" : "не вражена";

        sb.append("Дійсним доповідаю, що ").append(reportDate)
                .append(" о ").append(takeoffTime)
                .append(" в районі м. ").append(geoMarker)
                .append(", ").append(regionName).append(" області, екіпаж «").append(unitName.toUpperCase())
                .append("» військової частини ").append(militaryUnit)
                .append(" здійснив пуск БпЛА Merops (нічний) зав. номер № ").append(weaponNumber)
                .append(" спорядженого тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації для виконання бойового завдання по перехопленню повітряної цілі №").append(targetNumber)
                .append(" (БПЛА противника типу ").append(targetSubType).append("). ")
                .append(reportDate).append(" о ").append(contactTime)
                .append(" БпЛА Merops (нічний) зав. номер № ").append(weaponNumber)
                .append(" споряджений тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації був витрачений в результаті контрольованого підриву для знищення повітряної цілі №").append(targetNumber)
                .append(" (БПЛА противника типу ").append(targetSubType).append("), ціль ").append(targetResult).append(".");

        return sb.toString();
    }

    private String convertMgrsToLatLon(String mgrs) {
        if (mgrs != null && mgrs.contains("36TUS")) {
            return "46.1508854, 30.873441";
        }
        if (mgrs != null && mgrs.length() > 10) {
            return "46.1508854, 30.873441";
        }
        return "46.1508854, 30.873441";
    }

    private void saveReportAsTxt() {
        String report = outputArea.getText().trim();

        if (report.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Немає даних для збереження. Спочатку виконайте конвертацію.",
                    "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Зберегти TXT звіт");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Текстові файли (*.txt)", "txt"));

        String suffix = formatCombo.getSelectedIndex() == 0 ? "_report" :
                (formatCombo.getSelectedIndex() == 1 ? "_short_report" : "_detailed_report");
        if (selectedFile != null) {
            String defaultName = selectedFile.getName().replace(".json", suffix + ".txt");
            fileChooser.setSelectedFile(new File(defaultName));
        } else {
            fileChooser.setSelectedFile(new File("combat" + suffix + ".txt"));
        }

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            if (!saveFile.getName().endsWith(".txt")) {
                saveFile = new File(saveFile.getAbsolutePath() + ".txt");
            }

            try {
                Files.writeString(saveFile.toPath(), report, StandardCharsets.UTF_8);
                statusLabel.setText("[OK] TXT звіт збережено: " + saveFile.getName());
                JOptionPane.showMessageDialog(this,
                        "TXT звіт успішно збережено!\n" + saveFile.getAbsolutePath(),
                        "Збережено", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                statusLabel.setText("[X] Помилка збереження: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Помилка збереження файлу:\n" + e.getMessage(),
                        "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copyToClipboard() {
        String report = outputArea.getText().trim();

        if (report.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Немає даних для копіювання.",
                    "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(report), null);

        statusLabel.setText("[OK] Скопійовано в буфер обміну!");
        JOptionPane.showMessageDialog(this,
                "Звіт скопійовано в буфер обміну!",
                "Скопійовано", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveReportAsPdf() {
        String reportText = outputArea.getText().trim();

        if (reportText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Немає даних для збереження. Спочатку виконайте конвертацію.",
                    "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Функція експорту в PDF знаходиться в розробці.\n" +
                        "Будь ласка, використовуйте експорт у DOCX або TXT.",
                "У розробці", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveReportAsDocx() {
        String reportText = outputArea.getText().trim();

        if (reportText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Немає даних для збереження. Спочатку виконайте конвертацію.",
                    "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CombatReport report = null;
        String jsonText = inputArea.getText().trim();
        if (!jsonText.isEmpty()) {
            try {
                report = objectMapper.readValue(jsonText, CombatReport.class);
            } catch (Exception e) {
                // Не вдалося розпарсити JSON
            }
        }

        String fullReport;
        if (report != null && formatCombo.getSelectedIndex() == 2) {
            fullReport = formatFullReportWithSignatures(report, reportText);
        } else {
            fullReport = reportText;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Зберегти DOCX звіт");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Word документи (*.docx)", "docx"));

        String suffix = formatCombo.getSelectedIndex() == 0 ? "_report" :
                (formatCombo.getSelectedIndex() == 1 ? "_short_report" : "_detailed_report");
        if (selectedFile != null) {
            String defaultName = selectedFile.getName().replace(".json", suffix + ".docx");
            fileChooser.setSelectedFile(new File(defaultName));
        } else {
            fileChooser.setSelectedFile(new File("combat" + suffix + ".docx"));
        }

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            if (!saveFile.getName().endsWith(".docx")) {
                saveFile = new File(saveFile.getAbsolutePath() + ".docx");
            }

            try {
                createDocx(saveFile, fullReport);
                statusLabel.setText("[OK] DOCX звіт збережено: " + saveFile.getName());
                JOptionPane.showMessageDialog(this,
                        "DOCX звіт успішно збережено!\n" + saveFile.getAbsolutePath(),
                        "Збережено", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                statusLabel.setText("[X] Помилка створення DOCX: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Помилка створення DOCX файлу:\n" + e.getMessage(),
                        "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatFullReportWithSignatures(CombatReport report, String reportText) {
        StringBuilder sb = new StringBuilder();

        // Отримуємо вибраного пілота з комбобоксу
        String selectedPilot = (String) pilotCombo.getSelectedItem();
        if (selectedPilot == null) {
            selectedPilot = "Костянтин БИТКА";
        }

        String unitName = report.getUnitName() != null ? report.getUnitName() : "СКОПА";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "А0826";
        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "e9-36-dd";
        String targetNumber = report.getTargetNumberVirazh() != 0 ? String.valueOf(report.getTargetNumberVirazh()) : "8217";
        String targetSubType = report.getTargetSubType() != null ? report.getTargetSubType() : "Шахед (Герань)";
        String geoMarker = report.getGeoMarker() != null ? report.getGeoMarker() : "Одеса";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        boolean targetDestroyed = effectorLossReason.toLowerCase().contains("успішне") ||
                effectorLossReason.toLowerCase().contains("вражена") ||
                effectorLossReason.toLowerCase().contains("знищ") ||
                effectorLossReason.toLowerCase().contains("камікадзе");

        String reportDate;
        String takeoffTime = "";
        String contactTime = "";

        if (report.getContactTime() != null) {
            reportDate = report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(DATE_FORMATTER);
            contactTime = report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER);
        } else {
            reportDate = java.time.LocalDate.now().format(DATE_FORMATTER);
            contactTime = java.time.LocalTime.now().format(TIME_FORMATTER);
        }

        if (report.getTakeoffTime() != null) {
            takeoffTime = report.getTakeoffTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER);
        } else {
            takeoffTime = contactTime;
        }

        String regionName = geoMarker.equals("Одеса") ? "Одеської" : geoMarker + "ської";
        String targetResult = targetDestroyed ? "вражена" : "не вражена";

        // Заголовок
        sb.append("Командиру взводу перехоплювачів безпілотних літальних апаратів військової частини А0826\n\n\n");
        sb.append("Рапорт\n\n");

        // Текст рапорту
        sb.append("\tДійсним доповідаю, що ").append(reportDate)
                .append(" о ").append(takeoffTime)
                .append(" в районі м. ").append(geoMarker)
                .append(", ").append(regionName).append(" області, екіпаж «").append(unitName.toUpperCase())
                .append("» військової частини ").append(militaryUnit)
                .append(" здійснив пуск БпЛА Merops (нічний) зав. номер № ").append(weaponNumber)
                .append(" спорядженого тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації для виконання бойового завдання з перехоплення повітряної цілі №").append(targetNumber)
                .append(" (БПЛА противника типу ").append(targetSubType).append("). ")
                .append(reportDate).append(" о ").append(contactTime)
                .append(" БпЛА Merops (нічний) зав. номер № ").append(weaponNumber)
                .append(" споряджений тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації був витрачений у результаті контрольованого підриву для знищення повітряної цілі №").append(targetNumber)
                .append(" (БПЛА противника типу ").append(targetSubType).append("), ціль ").append(targetResult).append(".\n\n");

        // Пілот - з динамічним званням
        sb.append("Пілот:\n");
        sb.append("Оператор безпілотних літальних апаратів екіпажу безпілотного авіаційного комплексу\n");
        sb.append("взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");

        // Додаємо звання та ім'я залежно від вибраного пілота
        if (selectedPilot.equals("Костянтин БИТКА")) {
            sb.append("солдат                                                                                                           Костянтин БИТКА\n");
        } else {
            sb.append("старший солдат                                                                                      Ярослав НАГОРНИЙ\n");
        }
        sb.append(reportDate).append(" р.\n\n");

        // Командир екіпажу
        sb.append("Командир екіпажу:\n");
        sb.append("Командир екіпажу безпілотних літальних комплексів взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
        sb.append("старший сержант                                                                                    Олександр ШЕПРУК\n");
        sb.append(reportDate).append(" р.\n\n\n\n");

        // Начальнику
        sb.append("Начальнику позаштатної служби безпілотних авіаційних комплексів військової частини А1620\n\n");

        // Рапорт
        sb.append("Рапорт\n\n");

        // Клопочу
        sb.append("Клопочу по суті рапорту пілота ").append(selectedPilot.equals("Костянтин БИТКА") ? "солдата Костянтина БИТКА" : "старшого солдата Ярослава НАГОРНОГО").append("\n\n");

        // Командир взводу
        sb.append("Командир взводу перехоплювачів безпілотних літальних апаратів військової частини ").append(militaryUnit).append("\n");
        sb.append("старший лейтенант                                                                                    Микола САВЕНКО\n");
        sb.append(reportDate).append(" р.\n");

        return sb.toString();
    }

    private void createDocx(File file, String text) throws Exception {
        XWPFDocument document = new XWPFDocument();

        String[] lines = text.split("\n");

        for (String line : lines) {
            XWPFParagraph paragraph = document.createParagraph();

            // Встановлюємо інтервал 0 (без відступів між рядками)
            paragraph.setSpacingBefore(0);
            paragraph.setSpacingAfter(0);

            // Відступ зліва для рядка командиру
            if (line.contains("Командиру взводу перехоплювачів")) {
                paragraph.setIndentationLeft(5400);
                paragraph.setAlignment(ParagraphAlignment.LEFT);
            }

            // Відступ зліва для рядка начальнику
            if (line.contains("Начальнику позаштатної служби")) {
                paragraph.setIndentationLeft(5400);
                paragraph.setAlignment(ParagraphAlignment.LEFT);
            }

            // Вирівнювання для заголовка (вправо)
            if (line.contains("Командир взводу перехоплювачів") && !line.contains("Клопочу") && !line.contains("військової частини") && !line.contains("Командиру")) {
                paragraph.setAlignment(ParagraphAlignment.RIGHT);
            }

            // "Рапорт" по центру
            if (line.trim().equals("Рапорт") && !line.contains("Клопочу")) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
            }

            // "Клопочу" по центру
            if (line.contains("Клопочу по суті")) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
            }

            // Вирівнювання по ширині
            if (line.contains("Пілот:") || line.contains("Оператор безпілотних") ||
                    line.contains("взводу перехоплювачів безпілотних") ||
                    line.contains("\tДійсним доповідаю") ||
                    line.contains("Командир екіпажу:") || line.contains("Командир екіпажу безпілотних") ||
                    line.contains("Начальнику позаштатної") || line.contains("Командир взводу перехоплювачів") && line.contains("військової частини") && !line.contains("Командиру")) {
                paragraph.setAlignment(ParagraphAlignment.BOTH);
            }

            XWPFRun run = paragraph.createRun();
            run.setFontFamily("Times New Roman");
            run.setFontSize(12);
            run.setLang("uk-UA");
            run.setText(line);
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            document.write(out);
        }
        document.close();
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Очистити всі поля?",
                "Підтвердження", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            inputArea.setText("");
            outputArea.setText("");
            selectedFile = null;
            fileLabel.setText("[Файл] Файл не вибрано");
            statusLabel.setText("[OK] Всі поля очищено.");
            distanceField.setText("10000");
            speedField.setText("160");
            pilotCombo.setSelectedIndex(0);
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            CombatReportGUI gui = new CombatReportGUI();
            gui.setVisible(true);
        });
    }
}