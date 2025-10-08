package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Stop;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.concurrent.Task;



public class HeatmapTabController implements HasMainController {


    @FXML private ScrollPane heatmapScrollPane;
    @FXML private ComboBox<String> normalizationMethodBox;
    @FXML private ComboBox<String> sortingComboBox;
    @FXML private ComboBox<String> normalizationSourceBox;
    @FXML private CheckBox showValuesCheckBox;
    @FXML private ColorPicker minColorPicker;
    @FXML private ColorPicker maxColorPicker;
    @FXML private Label selectedFastaLabel;
    @FXML private StackPane heatmapContainer;
    @FXML private StackPane scrollWrapper;
    @FXML private CheckBox showXCheckbox;
    @FXML private CheckBox showGapsCheckbox;





    private Map<Integer, Map<Character, Integer>> lastPositionLetterCounts;
    private Map<Character, Integer> totalLetterCount = new HashMap<>();
    private Map<Character, Integer> externalLetterCount = new HashMap<>();
    private boolean useExternalNormalization = false;
    private boolean showX = false;
    private Color minColor = Color.WHITE;
    private Color maxColor = Color.RED;
    private List<Stop> customStops = null;
    private HBox currentHeatmapRow;
    private Map<Integer, Map<Character, Double>> zScoreMatrix = new HashMap<>();
    private int cellWidth = 40;
    private int cellHeight = 30;
    private MainController mainController;
    private Map<Integer, Map<Character, Double>> rawMatrix;
    private Map<Integer, Map<Character, Double>> directMatrix;
    private Map<Integer, Map<Character, Double>> percentMatrix;


    public Map<Integer, Map<Character, Double>> getRawMatrix() {
        return rawMatrix;
    }

    public Map<Integer, Map<Character, Double>> getDirectMatrix() {
        return directMatrix;
    }

    public Map<Integer, Map<Character, Double>> getPercentMatrix() {
        return percentMatrix;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    public Map<Integer, Map<Character, Double>> getZScoreMatrix() {
        return zScoreMatrix;
    }
    public ComboBox<String> getNormalizationMethodBox() {
        return normalizationMethodBox;
    }
    public void updateHeatmapFromLast() {
        updateHeatmap(lastPositionLetterCounts);
    }



    @FXML
    public void initialize() {
        heatmapScrollPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateCellSizeBasedOnHeight();
        });
        Platform.runLater(() -> updateCellSizeBasedOnHeight());

        normalizationMethodBox.getItems().setAll(
                "Z-score (global)",
                "Z-score (row)",
                "Direct normalization",
                "Percentage (per residue)",
                "Raw values"
        );
        normalizationMethodBox.setValue("Z-score (global)");
        normalizationMethodBox.setOnAction(e -> updateHeatmap(lastPositionLetterCounts));

        sortingComboBox.getItems().addAll("Default", "Alphabetical", "Hydrophobicity", "Polarity");
        sortingComboBox.setValue("Default");
        sortingComboBox.setOnAction(e -> updateHeatmap(lastPositionLetterCounts));

        normalizationMethodBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mainController != null && mainController.getLogoTabController() != null) {
                mainController.getLogoTabController().setLogoComboBoxValue(newVal);
            }
        });

        normalizationSourceBox.getItems().addAll(
                "Dataset composition",
                "Choose FASTA file",
                "Paste custom composition",
                "Swiss-Prot: Homo sapiens",
                "Swiss-Prot: Mus musculus",
                "Swiss-Prot: Escherichia coli",
                "Swiss-Prot: Custom taxon ID"
        );
        normalizationSourceBox.setValue("Dataset composition");

        normalizationSourceBox.setOnAction(e -> {
            String selected = normalizationSourceBox.getValue();
            if (selected.equals("Dataset composition")) {
                useExternalNormalization = false;

                updateHeatmap(lastPositionLetterCounts);
            } else if (selected.equals("Choose FASTA file")) {
                handleNormalizationSource();
            } else if (selected.equals("Paste custom composition")) {
                handleNormalizationSource();
            } else if (selected.startsWith("Swiss-Prot: ")) {
                if (selected.contains("Homo sapiens")) {
                    fetchAndUseSwissProtAsync("9606", "Homo sapiens");
                } else if (selected.contains("Mus musculus")) {
                    fetchAndUseSwissProtAsync("10090", "Mus musculus");
                } else if (selected.contains("Escherichia coli")) {
                    fetchAndUseSwissProtAsync("562", "Escherichia coli");
                }
                else if (selected.equals("Swiss-Prot: Custom taxon ID")) {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Enter Taxon ID");
                    dialog.setHeaderText("Download Swiss-Prot FASTA");
                    dialog.setContentText(
                            "Enter a valid NCBI Taxon ID to retrieve a proteome from UniProt.\n" +
                                    "Example: 9606 for Homo sapiens, 3702 for Arabidopsis thaliana.\n" +
                                    "Note: Only taxa present in the UniProt database will return results."
                    );

                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(taxonId -> {
                        if (taxonId.matches("\\d+")) {
                            fetchAndUseSwissProtAsync(taxonId, "Custom Taxon ID: " + taxonId);
                        } else {
                            mainController.setStatus("Invalid Taxon ID. Must be a number.");
                        }
                    });
                }
            }
        });
        showValuesCheckBox.setSelected(false);
        showValuesCheckBox.setOnAction(e -> updateHeatmap(lastPositionLetterCounts));

        minColorPicker.setValue(minColor);
        maxColorPicker.setValue(maxColor);

        minColorPicker.setOnAction(e -> {
            minColor = minColorPicker.getValue();
            updateHeatmap(lastPositionLetterCounts);
        });
        showXCheckbox.setSelected(false);
        showXCheckbox.setOnAction(e -> {
            showX = showXCheckbox.isSelected();
            updateHeatmap(lastPositionLetterCounts);
        });
        showGapsCheckbox.setSelected(false);
        showGapsCheckbox.setOnAction(e -> updateHeatmap(lastPositionLetterCounts));

        maxColorPicker.setOnAction(e -> {
            maxColor = maxColorPicker.getValue();
            updateHeatmap(lastPositionLetterCounts);
        });
        setupGradientPresets();

    }
    private void updateCellSizeBasedOnHeight() {
        double availableHeight = heatmapScrollPane.getHeight();

        if (lastPositionLetterCounts == null || lastPositionLetterCounts.isEmpty()) return;

        int numAAs = getSortedAminoAcids().size();
        int padding = 100;
        double targetHeight = availableHeight - padding;


        cellHeight = (int) Math.max(10, targetHeight / numAAs);


        cellWidth = (int) (cellHeight * 4.0 / 3.0);

        updateHeatmap(lastPositionLetterCounts);
    }


    @FXML
    private Hyperlink link;

    @FXML
    private void handleLinkClick() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.uniprot.org/taxonomy/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchAndUseSwissProtAsync(String taxonId, String taxonName) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String queryUrl = "https://rest.uniprot.org/uniprotkb/stream?compressed=false&format=fasta&query=organism_id:" + taxonId;
                HttpURLConnection conn = (HttpURLConnection) new URL(queryUrl).openConnection();
                conn.setRequestProperty("User-Agent", "CS");

                int contentLength = conn.getContentLength();
                InputStream inputStream = conn.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    if (contentLength > 0) {
                        updateProgress(totalRead, contentLength);
                    }
                }

                String fasta = outputStream.toString(StandardCharsets.UTF_8);


                File file = new File("swissprot_" + taxonId + ".fasta");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(fasta);
                }

                Map<Character, Integer> freqs = computeAminoAcidFrequenciesFromFastaString(fasta);

                Platform.runLater(() -> {
                    externalLetterCount = freqs;
                    useExternalNormalization = true;
                    updateHeatmap(lastPositionLetterCounts);
                    mainController.setStatus("SwissProt: " + taxonName + " loaded and saved successfully.");
                });

                return null;
            }

            @Override
            protected void scheduled() {
                Platform.runLater(() -> {
                    ProgressBar bar = mainController.getSwissprotProgressBar();
                    bar.progressProperty().unbind();
                    bar.setProgress(0);
                    bar.progressProperty().bind(this.progressProperty());
                    bar.setVisible(true);
                    mainController.setStatus("Downloading SwissProt FASTA...");
                });
            }


            @Override
            protected void succeeded() {
                Platform.runLater(() -> mainController.getSwissprotProgressBar().setVisible(false));
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                ex.printStackTrace();
                Platform.runLater(() -> {
                    mainController.getSwissprotProgressBar().setVisible(false);
                    mainController.setStatus("Failed to load SwissProt FASTA: " + ex.getMessage());
                });
            }
        };


        mainController.getSwissprotProgressBar().progressProperty().unbind();
        mainController.getSwissprotProgressBar().progressProperty().bind(task.progressProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }



    private Map<Character, Integer> computeAminoAcidFrequenciesFromFastaString(String fastaData) {
        Map<Character, Integer> frequencies = new HashMap<>();
        String[] lines = fastaData.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith(">")) continue;

            for (char c : line.toCharArray()) {
                char aa = isStandardAminoAcid(c) ? c : 'X';
                frequencies.put(aa, frequencies.getOrDefault(aa, 0) + 1);
            }
        }
        return frequencies;
    }


    private List<Character> getSortedAminoAcids() {
        List<Character> aaList;
        String sort = sortingComboBox.getValue();
        String defaultSet = showX ? "AVLIMPFYWGSTCQNKRHEDX" : "AVLIMPFYWGSTCQNKRHED";

        switch (sort) {
            case "Alphabetical":
                aaList = new ArrayList<>();
                for (char c : defaultSet.toCharArray()) aaList.add(c);
                Collections.sort(aaList);
                return aaList;

            case "Hydrophobicity":
                return stringToCharList(showX ? "IVLFCMAWGTSYPHNDQEKRX" : "IVLFCMAWGTSYPHNDQEKR");

            case "Polarity":
                return stringToCharList(showX ? "GAVLIMFWPSTCYNQDEKHRX" : "GAVLIMFWPSTCYNQDEKHR");

            default:
                return stringToCharList(defaultSet);
        }
    }


    private List<Character> stringToCharList(String s) {
        List<Character> list = new ArrayList<>();
        for (char c : s.toCharArray()) {
            list.add(c);
        }
        return list;
    }
    private List<Stop> reverseStops(List<Stop> stops) {
        List<Stop> reversed = new ArrayList<>();
        for (Stop stop : stops) {
            reversed.add(new Stop(1.0 - stop.getOffset(), stop.getColor()));
        }
        return reversed;
    }


    @FXML private ComboBox<String> gradientComboBox;

    private final Map<String, List<Stop>> gradientPresets = Map.of(
            "White → Red", List.of(new Stop(0, Color.WHITE), new Stop(1, Color.RED)),
            "Blue → Red", List.of(new Stop(0, Color.NAVY), new Stop(1, Color.RED)),
            "White → Yellow → Red → Black", List.of(
                    new Stop(0.0, Color.WHITE),
                    new Stop(0.33, Color.YELLOW),
                    new Stop(0.66, Color.RED),
                    new Stop(1.0, Color.BLACK)
            )
    );

    private void setupGradientPresets() {
        gradientComboBox.getItems().addAll(
                "White → Red", "Red → Yellow", "White → Purple",
                "Navy → Red", "Violet → Lime", "Scientific Reds"
        );
        gradientComboBox.setValue("White → Red");

        gradientComboBox.setOnAction(e -> {
            customStops = null;

            switch (gradientComboBox.getValue()) {
                case "White → Red" -> {
                    minColor = Color.WHITE;
                    maxColor = Color.RED;
                }
                case "Red → Yellow" -> {
                    minColor = Color.RED;
                    maxColor = Color.YELLOW;
                }
                case "White → Purple" -> {
                    minColor = Color.WHITE;
                    maxColor = Color.PURPLE;
                }
                case "Navy → Red" -> {
                    minColor = Color.NAVY;
                    maxColor = Color.RED;
                }
                case "Violet → Lime" -> {
                    minColor = Color.web("#9200ff");
                    maxColor = Color.LIME;
                }
                case "Scientific Reds" -> {
                    Color[] reds = {
                            Color.web("#fff5f0"), Color.web("#fee0d2"), Color.web("#fcbba1"),
                            Color.web("#fc9272"), Color.web("#fb6a4a"), Color.web("#ef3b2c"),
                            Color.web("#cb181d"), Color.web("#a50f15"), Color.web("#67000d"),
                            Color.web("#490006")
                    };
                    customStops = new ArrayList<>();
                    for (int i = 0; i < reds.length; i++) {
                        customStops.add(new Stop((double) i / (reds.length - 1), reds[i]));
                    }
                }
            }
            updateHeatmap(lastPositionLetterCounts);
        });
    }



    public void setData(Map<Integer, Map<Character, Integer>> positionLetterCounts, Map<Character, Integer> totalLetterCount) {
        this.lastPositionLetterCounts = positionLetterCounts;
        this.totalLetterCount = totalLetterCount;
        updateHeatmap(positionLetterCounts);
    }

    private void handleNormalizationSource() {
        String selected = normalizationSourceBox.getValue();
        if (selected.equals("Dataset composition")) {
            useExternalNormalization = false;
            selectedFastaLabel.setText("");
            updateHeatmap(lastPositionLetterCounts);
        } else if (selected.equals("Choose FASTA file")) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select FASTA File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FASTA Files", "*.fasta", "*.fa", "*.faa"));
            File fastaFile = fileChooser.showOpenDialog(null);
            if (fastaFile != null) {
                externalLetterCount = computeAminoAcidFrequenciesFromFasta(fastaFile);
                useExternalNormalization = true;
                if (mainController != null) {
                    mainController.setStatus("Selected: " + fastaFile.getName());
                }
                updateHeatmap(lastPositionLetterCounts);
            } else {
                normalizationSourceBox.setValue("Dataset composition");
            }
        }
        else if (selected.equals("Paste custom composition")) {
            Stage dialog = new Stage();
            dialog.setTitle("Paste Custom Amino Acid Composition");

            TextArea textArea = new TextArea();
            textArea.setPromptText("Enter amino acid counts, one per line (e.g. A 100)");

            Button applyButton = new Button("Apply");
            applyButton.setOnAction(ev -> {
                Map<Character, Integer> customCounts = new HashMap<>();
                String[] lines = textArea.getText().split("\\n");
                for (String line : lines) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2) {
                        try {
                            char aa = parts[0].toUpperCase().charAt(0);
                            int count = Integer.parseInt(parts[1]);
                            if (isStandardAminoAcid(aa) || aa == 'X') {
                                customCounts.put(aa, count);
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (!customCounts.isEmpty()) {
                    externalLetterCount = customCounts;
                    useExternalNormalization = true;
                    if (mainController != null) {
                        mainController.setStatus("Custom composition loaded");
                    }
                    updateHeatmap(lastPositionLetterCounts);
                }

                dialog.close();
            });

            VBox dialogLayout = new VBox(10, new Label("Paste your custom amino acid composition (e.g. A 100):"), textArea, applyButton);
            dialogLayout.setPadding(new Insets(10));
            Scene scene = new Scene(dialogLayout, 400, 300);
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.showAndWait();
        }
    }

    private Map<Character, Integer> computeAminoAcidFrequenciesFromFasta(File fastaFile) {
        Map<Character, Integer> frequencies = new HashMap<>();
        try (Scanner scanner = new Scanner(fastaFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith(">")) continue;
                for (char c : line.toCharArray()) {
                    char aa = isStandardAminoAcid(c) ? c : 'X';
                    frequencies.put(aa, frequencies.getOrDefault(aa, 0) + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frequencies;
    }

    private boolean isStandardAminoAcid(char c) {
        return "ARNDCEQGHILKMFPSTWYV".indexOf(c) >= 0;
    }


    private void updateHeatmap(Map<Integer, Map<Character, Integer>> positionLetterCounts) {
        if (positionLetterCounts == null || positionLetterCounts.isEmpty()) return;
        List<Integer> positions = new ArrayList<>(positionLetterCounts.keySet());
        Collections.sort(positions);
        int minPos = positions.get(0);
        int maxPos = positions.get(positions.size() - 1);

        List<Character> aminoAcids = getSortedAminoAcids();
        String normMethod = normalizationMethodBox.getValue();


        Map<Integer, Map<Character, Double>> directNormalizedMatrix = new HashMap<>();

        for (int pos = minPos; pos <= maxPos; pos++) {
            Map<Character, Double> col = new HashMap<>();
            Map<Character, Integer> rawMap = positionLetterCounts.get(pos);
            for (char aa : "AVLIMPFYWGSTCQNKRHEDX".toCharArray()) {
                int rawCount = rawMap.getOrDefault(aa, 0);
                int total = useExternalNormalization
                        ? externalLetterCount.getOrDefault(aa, 1)
                        : totalLetterCount.getOrDefault(aa, 1);
                double value = (double) rawCount / total;
                col.put(aa, value);
            }
            directNormalizedMatrix.put(pos, col);
        }


        Map<Integer, Map<Character, Double>> normalizedMatrix = new HashMap<>();

        if (normMethod.equals("Raw values")) {

            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> col = new HashMap<>();
                Map<Character, Integer> rawMap = positionLetterCounts.get(pos);
                for (char aa : aminoAcids) {
                    col.put(aa, (double) rawMap.getOrDefault(aa, 0));
                }
                normalizedMatrix.put(pos, col);
            }
        } else if (normMethod.equals("Direct normalization")) {

            normalizedMatrix = directNormalizedMatrix;
        } else if ("Z-score (global)".equals(normMethod)) {

            List<Double> allValues = new ArrayList<>();
            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> col = directNormalizedMatrix.get(pos);
                for (char aa : aminoAcids) {
                    allValues.add(col.getOrDefault(aa, 0.0));
                }
            }
            double mean = allValues.stream().mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);
            double var = allValues.stream().mapToDouble(v -> {
                double d = v - mean;
                return d * d;
            }).average().orElse(0.0);
            double std = (var <= 0.0) ? 0.0 : Math.sqrt(var);

            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> out = new HashMap<>();
                Map<Character, Double> in = directNormalizedMatrix.get(pos);
                for (char aa : aminoAcids) {
                    double val = in.getOrDefault(aa, 0.0);
                    double z = (std == 0.0) ? 0.0 : (val - mean) / std;
                    out.put(aa, z);
                }
                normalizedMatrix.put(pos, out);
            }

        } else if ("Z-score (row)".equals(normMethod)) {

            Map<Character, List<Double>> rows = new HashMap<>();
            for (char aa : aminoAcids) rows.put(aa, new ArrayList<>());

            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> col = directNormalizedMatrix.get(pos);
                for (char aa : aminoAcids) {
                    rows.get(aa).add(col.getOrDefault(aa, 0.0));
                }
            }

            Map<Character, double[]> stats = new HashMap<>();
            for (char aa : aminoAcids) {
                List<Double> v = rows.get(aa);
                double mean = v.stream().mapToDouble(d -> d).average().orElse(0.0);
                double var = v.stream().mapToDouble(d -> {
                    double t = d - mean; return t * t;
                }).average().orElse(0.0);
                double std = (var <= 0.0) ? 0.0 : Math.sqrt(var);
                stats.put(aa, new double[]{mean, std});
            }

            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> outCol = new HashMap<>();
                Map<Character, Double> inCol  = directNormalizedMatrix.get(pos);
                for (char aa : aminoAcids) {
                    double val = inCol.getOrDefault(aa, 0.0);
                    double[] st = stats.get(aa);
                    double z = (st[1] == 0.0) ? 0.0 : (val - st[0]) / st[1];
                    outCol.put(aa, z);
                }
                normalizedMatrix.put(pos, outCol);
            }

        } else if ("Percentage (per residue)".equals(normMethod)) {

            Map<Character, Double> rowSums = new HashMap<>();
            for (char aa : aminoAcids) rowSums.put(aa, 0.0);


            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> col = directNormalizedMatrix.get(pos);
                for (char aa : aminoAcids) {
                    rowSums.put(aa, rowSums.get(aa) + col.getOrDefault(aa, 0.0));
                }
            }



            for (int pos = minPos; pos <= maxPos; pos++) {
                Map<Character, Double> outCol = new HashMap<>();
                Map<Character, Double> inCol  = directNormalizedMatrix.get(pos);
                for (char aa : aminoAcids) {
                    double sum = rowSums.getOrDefault(aa, 0.0);
                    double val = inCol.getOrDefault(aa, 0.0);
                    double pct = (sum == 0.0) ? 0.0 : (val * 100.0) / sum;
                    outCol.put(aa, pct);
                }
                normalizedMatrix.put(pos, outCol);
            }

        }

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int pos = minPos; pos <= maxPos; pos++) {
            Map<Character, Double> col = normalizedMatrix.get(pos);
            for (char aa : aminoAcids) {
                double v = col.getOrDefault(aa, 0.0);
                if (v < min) min = v;
                if (v > max) max = v;
            }



            LogoTabController logoTab = mainController.getLogoTabController();
            if (logoTab != null) {
                if ("Z-score (global)".equals(normMethod) || "Z-score (row)".equals(normMethod)) {
                    logoTab.setZScoreMatrix(normalizedMatrix);
                    logoTab.setSorting(sortingComboBox.getValue());
                    logoTab.setShowX(showXCheckbox.isSelected());


                } else {
                    logoTab.clearZScoreMatrix();
                }
            }



        }


        VBox sortingScale = new VBox();
        sortingScale.setAlignment(Pos.CENTER_LEFT);
        Label sortLabel = new Label();
        String sorting = sortingComboBox.getValue();
        if (sorting.equals("Hydrophobicity")) {
            sortLabel.setText("Hydrophobic ↑\nHydrophilic ↓");
        } else if (sorting.equals("Polarity")) {
            sortLabel.setText("Non-polar ↑\nPolar ↓");
        }

        sortLabel.setStyle("-fx-font-size: " + (int)(cellHeight * 0.4) + "px;");
        if (!sortLabel.getText().isEmpty()) {
            sortingScale.getChildren().add(sortLabel);
        }

        GridPane grid = new GridPane();
        if (showGapsCheckbox.isSelected()) {
            grid.setHgap(1);
            grid.setVgap(1);
        }
        List<String> positionLabels = new ArrayList<>();
        for (int pos = minPos; pos <= maxPos; pos++) {
            if (pos < 0) {
                positionLabels.add("P" + (-pos+1));
            } else if (pos == 0) {
                positionLabels.add("P1");
            } else {
                positionLabels.add("P" + pos + "’");
            }
        }

        for (int col = minPos; col <= maxPos; col++) {
            Label label = new Label(positionLabels.get(col - minPos));
            label.setMaxWidth(cellWidth);
            label.setMinWidth(cellWidth);
            label.setAlignment(Pos.CENTER);
            label.setStyle("-fx-font-size: " + (int)(cellHeight * 0.4) + "px;");
            grid.add(label, col - minPos + 1, 0);
        }


        for (int row = 0; row < aminoAcids.size(); row++) {
            Label label = new Label(String.valueOf(aminoAcids.get(row)));
            label.setMinHeight(cellHeight);
            label.setPrefHeight(cellHeight);
            label.setAlignment(Pos.CENTER);
            label.setStyle("-fx-font-size: " + (int)(cellHeight * 0.4) + "px;");
            grid.add(label, 0, row + 1);
        }


        for (int col = minPos; col <= maxPos; col++) {
            Map<Character, Double> colMap = normalizedMatrix.get(col);
            for (int row = 0; row < aminoAcids.size(); row++) {
                char aa = aminoAcids.get(row);
                double value = colMap.getOrDefault(aa, 0.0);
                double intensity = (max == min) ? 0 : (value - min) / (max - min);
                Color color = interpolateColor(minColor, maxColor, intensity);

                Region cell = new Region();
                cell.setPrefSize(cellWidth, cellHeight);
                cell.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));

                StackPane cellContainer = new StackPane(cell);
                if (showValuesCheckBox.isSelected()) {
                    String text;
                    if ("Raw values".equals(normMethod)) {
                        text = String.valueOf((int) value);
                    } else if ("Percentage (per residue)".equals(normMethod)) {
                        text = String.format(Locale.US, "%.1f%%", value);
                    } else if ("Direct normalization".equals(normMethod)) {
                        text = String.format(Locale.US, "%.4f", value);
                    } else {

                        text = String.format(Locale.US, "%.2f", value);
                    }
                    Label valueLabel = new Label(text);

                    valueLabel.setStyle("-fx-font-size: " + (int)(cellHeight * 0.35) + "px;");
                    double brightness = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
                    valueLabel.setTextFill(brightness < 0.5 ? Color.WHITE : Color.BLACK);
                    cellContainer.getChildren().add(valueLabel);
                }
                grid.add(cellContainer, col - minPos + 1, row + 1);
            }
        }

        VBox colorScale = new VBox(2);
        colorScale.setAlignment(Pos.CENTER);
        Label maxLabel;
        Label minLabel;
        if (normMethod.equals("Raw values")) {
            maxLabel = new Label(String.valueOf((int) max));
            minLabel = new Label(String.valueOf((int) min));
        }else if (normMethod.equals("Percentage (per residue)")){
            maxLabel = new Label(String.format(Locale.US, "%.1f%%", max));
            minLabel = new Label(String.format(Locale.US, "%.1f%%", min));
        } else {
            maxLabel = new Label(String.format(Locale.US, "%.2f", max));
            minLabel = new Label(String.format(Locale.US, "%.2f", min));
        }
        double labelFontSize = cellHeight * 0.4;
        String fontStyle = "-fx-font-size: " + (int) labelFontSize + "px;";
        maxLabel.setStyle(fontStyle);
        minLabel.setStyle(fontStyle);


        Rectangle gradient = new Rectangle();

        gradient.widthProperty().bind(colorScale.widthProperty().multiply(0.8));

        gradient.widthProperty().addListener((obs, oldVal, newVal) -> {
            gradient.setHeight(newVal.doubleValue() * 10);
        });
        LinearGradient lg;
        if (customStops != null) {
            lg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, reverseStops(customStops));
        } else {
            Stop[] stops = new Stop[]{
                    new Stop(1, minColor),
                    new Stop(0, maxColor)
            };
            lg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        }


        gradient.setFill(lg);
        colorScale.getChildren().addAll(maxLabel, gradient, minLabel);


        HBox heatmapRow = new HBox(10, sortingScale, grid, colorScale);
        heatmapRow.setAlignment(Pos.CENTER);
        heatmapRow.setMaxWidth(Region.USE_PREF_SIZE);


        currentHeatmapRow = heatmapRow;


        this.directMatrix = directNormalizedMatrix;


        this.rawMatrix = new HashMap<>();
        for (int pos : positionLetterCounts.keySet()) {
            Map<Character, Double> rawCol = new HashMap<>();
            Map<Character, Integer> rawSource = positionLetterCounts.get(pos);
            for (Character aa : rawSource.keySet()) {
                rawCol.put(aa, rawSource.get(aa).doubleValue());
            }
            this.rawMatrix.put(pos, rawCol);
        }


        this.percentMatrix = new HashMap<>();
        for (int pos : positionLetterCounts.keySet()) {
            Map<Character, Integer> rawSource = positionLetterCounts.get(pos);
            int total = rawSource.values().stream().mapToInt(Integer::intValue).sum();
            Map<Character, Double> percentCol = new HashMap<>();
            for (Character aa : rawSource.keySet()) {
                percentCol.put(aa, total > 0 ? 100.0 * rawSource.get(aa) / total : 0.0);
            }
            this.percentMatrix.put(pos, percentCol);
        }




        HBox centeredBox = new HBox(heatmapRow);
        centeredBox.setAlignment(Pos.CENTER);
        centeredBox.setFillHeight(false);
        centeredBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        centeredBox.setMaxWidth(Region.USE_PREF_SIZE);


        heatmapContainer.getChildren().clear();
        heatmapContainer.getChildren().add(centeredBox);

        heatmapScrollPane.setFitToWidth(true);
        heatmapScrollPane.setPannable(true);
        heatmapScrollPane.setContent(heatmapContainer);


        heatmapRow.setAlignment(Pos.CENTER);
        StackPane.setAlignment(heatmapRow, Pos.CENTER);

    }


    private Color interpolateColor(Color start, Color end, double t) {
        if (customStops != null && !customStops.isEmpty()) {

            for (int i = 0; i < customStops.size() - 1; i++) {
                Stop s1 = customStops.get(i);
                Stop s2 = customStops.get(i + 1);
                if (t >= s1.getOffset() && t <= s2.getOffset()) {
                    double localT = (t - s1.getOffset()) / (s2.getOffset() - s1.getOffset());
                    return s1.getColor().interpolate(s2.getColor(), localT);
                }
            }
            return customStops.get(customStops.size() - 1).getColor();
        } else {
            double r = start.getRed() + (end.getRed() - start.getRed()) * t;
            double g = start.getGreen() + (end.getGreen() - start.getGreen()) * t;
            double b = start.getBlue() + (end.getBlue() - start.getBlue()) * t;
            return new Color(r, g, b, 1.0);
        }
    }

    @FXML
    private void exportHeatmapAsPng() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Heatmap as PNG");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        fileChooser.setInitialFileName("heatmap.png");
        File file = fileChooser.showSaveDialog(null);

        if (file != null && currentHeatmapRow != null) {

            currentHeatmapRow.applyCss();
            currentHeatmapRow.layout();


            WritableImage image = currentHeatmapRow.snapshot(new SnapshotParameters(), null);

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                System.out.println("Heatmap image saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private Color getTextColorBasedOnBg(Color bg) {
        double brightness = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return brightness < 0.5 ? Color.WHITE : Color.BLACK;
    }

}
