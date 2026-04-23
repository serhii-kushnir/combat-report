package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.File;
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

    // Поля для ручного введення
    private JTextField distanceField;  // Відстань від місця взльоту
    private JTextField speedField;      // Швидкість цілі

    public CombatReportGUI() {
        setTitle("Конвертер бойових звітів JSON -> TXT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 850);
        setLocationRelativeTo(null);

        initComponents();
    }

    // Метод для обмеження введення тільки цифрами
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

        // Панель для полів вводу (розташована ЗВЕРХУ перед кнопками)
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Параметри звіту"));

        // Поле для відстані
        JLabel distanceLabel = new JLabel("Відстань від місця взльоту (м):");
        distanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(distanceLabel);

        distanceField = new JTextField("10000", 8);
        distanceField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        distanceField.setPreferredSize(new Dimension(100, 28));
        distanceField.setToolTipText("Відстань від місця взльоту в метрах (тільки цифри)");
        setNumericOnly(distanceField);
        inputPanel.add(distanceField);

        // Поле для швидкості
        JLabel speedLabel = new JLabel("Швидкість цілі (км/год):");
        speedLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(speedLabel);

        speedField = new JTextField("160", 8);
        speedField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        speedField.setPreferredSize(new Dimension(80, 28));
        speedField.setToolTipText("Швидкість цілі в км/год (тільки цифри)");
        setNumericOnly(speedField);
        inputPanel.add(speedField);

        // Панель для кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Дії"));

        // Вибір формату виводу
        JLabel formatLabel = new JLabel("Формат звіту:");
        formatLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        buttonPanel.add(formatLabel);

        String[] formats = {"[1] Стандартний формат", "[2] Скорочений формат (Екіпаж/Ціль)", "[3] Детальний формат (Дійсним доповідаю)"};
        formatCombo = new JComboBox<>(formats);
        formatCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formatCombo.setPreferredSize(new Dimension(280, 28));
        buttonPanel.add(formatCombo);

        // Кнопка вибору файлу
        JButton fileButton = new JButton("[+] Вибрати JSON файл");
        fileButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fileButton.setBackground(new Color(59, 89, 182));
        fileButton.setForeground(Color.BLACK);
        fileButton.setFocusPainted(false);
        fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileButton.addActionListener(e -> chooseJsonFile());
        buttonPanel.add(fileButton);

        // Кнопка конвертації
        JButton convertButton = new JButton("[=>] Конвертувати");
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        convertButton.setBackground(new Color(0, 128, 0));
        convertButton.setForeground(Color.BLACK);
        convertButton.setFocusPainted(false);
        convertButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        convertButton.addActionListener(e -> convertToReport());
        buttonPanel.add(convertButton);

        // Кнопка збереження
        JButton saveButton = new JButton("[*] Зберегти звіт");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveButton.setBackground(new Color(255, 140, 0));
        saveButton.setForeground(Color.BLACK);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.addActionListener(e -> saveReport());
        buttonPanel.add(saveButton);

        // Кнопка копіювання
        JButton copyButton = new JButton("[C] Копіювати");
        copyButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        copyButton.setBackground(new Color(70, 130, 180));
        copyButton.setForeground(Color.BLACK);
        copyButton.setFocusPainted(false);
        copyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyButton.addActionListener(e -> copyToClipboard());
        buttonPanel.add(copyButton);

        // Кнопка очищення
        JButton clearButton = new JButton("[X] Очистити");
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearButton.setBackground(new Color(220, 20, 60));
        clearButton.setForeground(Color.BLACK);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearAll());
        buttonPanel.add(clearButton);

        // Панель для відображення файлу
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileLabel = new JLabel("[Файл] Файл не вибрано");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileLabel.setForeground(new Color(100, 100, 100));
        filePanel.add(fileLabel);

        // Збираємо все разом
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
        panel.setPreferredSize(new Dimension(panel.getWidth(), 60)); // Збільшено висоту

        // Ліва частина - статус
        statusLabel = new JLabel("[OK] Готовий до роботи. Виберіть JSON файл або вставте JSON вручну.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(Color.BLACK);
        panel.add(statusLabel, BorderLayout.WEST);

        // Права частина - авторські права
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

        // Отримуємо значення з ручних полів (вже гарантовано цифри)
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

        if (report.getContactTime() != null) {
            sb.append(report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(DATE_FORMATTER)).append("\n");
        }

        sb.append("Екіпаж: ").append(report.getUnitName()).append("\n");

        // Втрата борту - беремо з effectorStatus
        String effectorStatus = report.getEffectorStatus() != null ? report.getEffectorStatus() : "";
        sb.append(effectorStatus).append("\n");

        sb.append("\n");

        if (report.getTakeoffTime() != null) {
            sb.append("Час взльоту: ").append(report.getTakeoffTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER)).append("\n");
        }

        if (report.getContactTime() != null) {
            sb.append("Втрата борту: ").append(report.getContactTime()
                    .atZone(ZoneId.of("UTC"))
                    .format(TIME_FORMATTER)).append("\n");
        }

        String coordinates = convertMgrsToLatLon(report.getCoordinates());
        sb.append("Координати: ").append(coordinates).append("\n");

        // Відстань - беремо з ручного поля
        sb.append("Відстань від місця взльоту: ").append(manualDistance).append(" м\n");

        sb.append("Тип: ").append(report.getTargetType()).append("\n");

        // Ідентифікація: ЗАВЖДИ "Дружній"
        sb.append("Ідентифікація: Дружній\n");

        String weapon = report.getWeaponId() != null ?
                report.getWeaponId().replace(" (AS3 Merops)", "") : "Merops";
        sb.append("Засіб ураження: ").append(weapon)
                .append(" ").append(report.getWeaponNumber()).append("\n");

        sb.append("Вибухівка: ШИФР «3-1.2 КУФ» 1.2 кг (8g 35m H)\n");
        sb.append("Детонатор: Вбудована розумна плата ініціації.\n");

        // ========== ПРИМІТКА ==========
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

        // ========== ВИСОТА ТА ЦІЛЬ ==========
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

        // Отримуємо дані з JSON
        String unitName = report.getUnitName() != null ? report.getUnitName() : "";
        String militaryUnit = report.getMilitaryUnit() != null ? report.getMilitaryUnit() : "";
        String geoMarker = report.getGeoMarker() != null ? report.getGeoMarker() : "Одеса";
        String targetNumber = report.getTargetNumberVirazh() != 0 ? String.valueOf(report.getTargetNumberVirazh()) : "";
        String targetSubType = report.getTargetSubType() != null ? report.getTargetSubType() : "Шахед (Герань)";
        String weaponNumber = report.getWeaponNumber() != null ? report.getWeaponNumber() : "";
        String effectorLossReason = report.getEffectorLossReason() != null ? report.getEffectorLossReason() : "";

        // Визначаємо, чи ціль вражена на основі effectorLossReason
        boolean targetDestroyed = effectorLossReason.toLowerCase().contains("успішне") ||
                effectorLossReason.toLowerCase().contains("вражена") ||
                effectorLossReason.toLowerCase().contains("знищ") ||
                effectorLossReason.toLowerCase().contains("камікадзе");

        // Отримуємо дату та час з JSON
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

        // Виправляємо назву області (Одесаської -> Одеської)
        String regionName = geoMarker.equals("Одеса") ? "Одеської" : geoMarker + "ської";

        // ========== ДЕТАЛЬНИЙ ЗВІТ ==========
        sb.append("    Дійсним доповідаю що ").append(reportDate)
                .append(" в ").append(takeoffTime)
                .append(" в районі м.").append(geoMarker)
                .append(", ").append(regionName).append(" області, екіпаж «").append(unitName.toUpperCase())
                .append("» військової частини ").append(militaryUnit)
                .append(" здійснив пуск БпЛА Merops (нічний) зав. номер № ").append(weaponNumber)
                .append(" спорядженого тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації для виконання бойового завдання по перехопленню повітряної цілі №").append(targetNumber)
                .append(" (БПЛА противника типу ").append(targetSubType).append("). ")
                .append(reportDate).append(" в ").append(contactTime)
                .append(" БпЛА Merops (нічний) зав. номер № ").append(weaponNumber)
                .append(" споряджений тротиловою шашкою КУФ 1200 грам, та вбудованою розумною платою ініціації був витрачений в результаті контрольованого підриву для знищення повітряної цілі №").append(targetNumber)
                .append(" (БПЛА противника типу ").append(targetSubType).append("), ціль ");

        if (targetDestroyed) {
            sb.append("вражена.");
        } else {
            sb.append("не вражена.");
        }

        sb.append("\n");

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

    private void saveReport() {
        String report = outputArea.getText().trim();

        if (report.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Немає даних для збереження. Спочатку виконайте конвертацію.",
                    "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Зберегти звіт");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Текстові файли (*.txt)", "txt"));

        String suffix = formatCombo.getSelectedIndex() == 0 ? "_report" : "_short_report";
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
                statusLabel.setText("[OK] Звіт збережено: " + saveFile.getName());
                JOptionPane.showMessageDialog(this,
                        "Звіт успішно збережено!\n" + saveFile.getAbsolutePath(),
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