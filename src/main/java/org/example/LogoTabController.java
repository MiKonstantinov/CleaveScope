package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import java.util.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;


public class LogoTabController implements HasMainController {

    @FXML
    private WebView logoWebView;

    @FXML
    private Spinner<Double> heightScaleSpinner;
    private double heightPerZ = 10.0;
    @FXML
    private Spinner<Double> heightYSpinner;
    private double heightY = 2.0;
    @FXML
    private ComboBox<String> fontStyleComboBox;
    @FXML
    private ComboBox<String> colorSchemeBox;
    @FXML
    private CheckBox boldCheckbox;
    @FXML
    private CheckBox showLegendCheckbox;
    @FXML
    private ComboBox<String> logoModeComboBox;
    @FXML
    private ScrollPane scrollPane;

    @FXML
    public void initialize() {



        if (boldCheckbox != null) {
            boldCheckbox.setSelected(false);
            AAGlyphLibrary.setThinMode(true);
        }
        colorSchemeBox.getItems().addAll("Default","Group-based", "Hydrophobicity","Grayscale", "Random", "Custom");
        colorSchemeBox.setValue("Default");


        double minHeightPerZ = 2.0;
        double maxHeightPerZ = 40.0;
        double step = 0.5;
        double initial = (this.heightPerZ > 0) ? this.heightPerZ : 10.0;

        SpinnerValueFactory.DoubleSpinnerValueFactory vf =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(minHeightPerZ, maxHeightPerZ, initial, step);

        heightScaleSpinner.setValueFactory(vf);
        heightScaleSpinner.setEditable(true);


        heightScaleSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.heightPerZ = newVal;
                updateLogo();
            }
        });


        heightScaleSpinner.getEditor().setOnAction(e -> commitEditor(heightScaleSpinner));
        heightScaleSpinner.getEditor().focusedProperty().addListener((o, was, is) -> {
            if (!is) commitEditor(heightScaleSpinner);
        });


        double minCenterY = 1.0;
        double maxCenterY = 10.0;
        double stepCenterY = 0.1;
        double initialCenterY = (this.heightY > 0) ? this.heightY : 2.0;

        SpinnerValueFactory<Double> centerYFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(minCenterY, maxCenterY, initialCenterY, stepCenterY);

        heightYSpinner.setValueFactory(centerYFactory);
        heightYSpinner.setEditable(true);

        heightYSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.heightY = newVal;
                updateLogo();
            }
        });


        heightYSpinner.getEditor().setOnAction(e -> commitEditor(heightYSpinner));
        heightYSpinner.getEditor().focusedProperty().addListener((o, was, is) -> {
            if (!is) commitEditor(heightYSpinner);
        });


        logoModeComboBox.setValue("Z-score (global)");


        colorSchemeBox.setOnAction(e -> updateLogo());
        showLegendCheckbox.setOnAction(e -> updateLogo());


        updateLogo();
        scrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (zScoreMatrix != null && !zScoreMatrix.isEmpty()) {
                double width = newVal.doubleValue();
                if (width > 100) {
                    updateLogoWithWidth(width);
                }
            }
        });
    }

    private void updateLogoWithWidth(double width) {
        String svg = generateSequenceLogoSVG(zScoreMatrix, width);
        String html = "<html><body style='margin:0; padding:0; display:flex; justify-content:center; align-items:center; height:100vh;'>"
                + svg + "</body></html>";
        logoWebView.getEngine().loadContent(html, "text/html");
    }




    private Map<Integer, Map<Character, Double>> directMatrix;
    private Map<Integer, Map<Character, Double>> percentMatrix;
    private Map<Integer, Map<Character, Double>> rawMatrix;
    public void setDirectMatrix(Map<Integer, Map<Character, Double>> matrix) {
        this.directMatrix = matrix;
    }
    public void setPercentMatrix(Map<Integer, Map<Character, Double>> matrix) {
        this.percentMatrix = matrix;
    }
    public void setRawMatrix(Map<Integer, Map<Character, Double>> matrix) {
        this.rawMatrix = matrix;
    }


/*    private Map<Integer, Map<Character, Double>> percentageDiffMatrix = new HashMap<>();

    public void setPercentageDiffMatrix(Map<Integer, Map<Character, Double>> matrix) {
        this.percentageDiffMatrix = matrix;
        this.zScoreMatrix.clear();
        updateLogo();
    }

    public void clearPercentageDiffMatrix() {
        this.percentageDiffMatrix.clear();
    }
*/

    private Map<Integer, Map<Character, Double>> zScoreMatrix = new HashMap<>();
    private boolean showX = false;
    private String sorting = "Default";
    private MainController mainController;


    private final Map<Character, Color> customColorMap = new HashMap<>();
    private int leftPositions = 6;
    private int rightPositions = 5;

    public void setFlanks(int left, int right) {
        this.leftPositions = left;
        this.rightPositions = right;
    }
    public void clearZScoreMatrix() {
        this.zScoreMatrix.clear();
        if (logoWebView != null) {
            this.logoWebView.getEngine().loadContent("");
        }
    }


    private void drawSequenceLogo(Map<Integer, Map<Character, Double>> matrix) {

        System.out.println("Drawing logo for matrix with " + matrix.size() + " positions");

    }


    public void setLogoComboBoxValue(String value) {
        if (logoModeComboBox != null) {
            logoModeComboBox.setValue(value);
        }
    }

    @FXML
    private void updateLogoMode() {
        if (mainController == null || mainController.getHeatmapTabController() == null) return;

        String mode = logoModeComboBox.getValue();

        mainController.getHeatmapTabController().getNormalizationMethodBox().setValue(mode);


        mainController.getHeatmapTabController().updateHeatmapFromLast();
        Map<Integer, Map<Character, Double>> matrix;

        switch (mode) {
            case "Raw values":
                matrix = mainController.getHeatmapTabController().getRawMatrix();
                break;
            case "Direct normalization":
                matrix = mainController.getHeatmapTabController().getDirectMatrix();
                break;
            case "Percentage (per residue)":
                matrix = mainController.getHeatmapTabController().getPercentMatrix();
                break;
            case "Z-score (global)":
            default:
                matrix = mainController.getHeatmapTabController().getZScoreMatrix();
                break;
        }
        if (matrix != null) {
            drawSequenceLogo(matrix);
        }
        updateLogo();
    }

    private double[] calculateZScoreExtremesPerColumn(Map<Integer, Map<Character, Double>> zScoreMatrix) {
        double maxPositiveSum = 0.0;
        double maxNegativeSum = 0.0;

        for (Map<Character, Double> column : zScoreMatrix.values()) {
            double posSum = 0.0;
            double negSum = 0.0;

            for (double z : column.values()) {
                if (z > 0) posSum += z;
                else if (z < 0) negSum += z;
            }

            if (posSum > maxPositiveSum) maxPositiveSum = posSum;
            if (negSum < maxNegativeSum) maxNegativeSum = negSum;
        }

        return new double[]{maxNegativeSum, maxPositiveSum};
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setZScoreMatrix(Map<Integer, Map<Character, Double>> matrix) {
        this.zScoreMatrix = matrix;


        Platform.runLater(this::updateLogo);
    }


    private void updateLogo() {
        String logoMode = logoModeComboBox.getValue();
        if (!"Z-score (global)".equals(logoMode) && !"Z-score (row)".equals(logoMode)) {

            String html = """
        <html>
        <body style='margin:0; padding:0; display:flex; justify-content:center; align-items:center; height:100vh;'>
            <div style='font-family:Arial,sans-serif; font-size:64px; font-weight:bold; color:#888;'>NA</div>
        </body>
        </html>
        """;
            logoWebView.getEngine().loadContent(html, "text/html");
            return;
        }

        if (zScoreMatrix == null || zScoreMatrix.isEmpty()) return;
        double webViewWidth = logoWebView.getWidth();
        if (webViewWidth < 100) {
            webViewWidth = scrollPane.getWidth();
        }
        if (webViewWidth < 100) {
            webViewWidth = 800;
        }


        String svg = generateSequenceLogoSVG(zScoreMatrix, webViewWidth);


        String html = "<html><body style='margin:0; padding:0; display:flex; justify-content:center; align-items:center; height:100vh;'>"
                + svg + "</body></html>";

        logoWebView.getEngine().loadContent(html, "text/html");
    }


    @FXML
    private void handleUpdateLogo() {

        commitEditor(heightScaleSpinner);
        Double val = heightScaleSpinner.getValue();
        if (val != null) {
            heightPerZ = val;
            updateLogo();
        }
    }
    private static void commitEditor(Spinner<Double> spinner) {
        try {
            String txt = spinner.getEditor().getText();
            if (txt == null) return;
            double v = Double.parseDouble(txt.trim().replace(',', '.'));
            SpinnerValueFactory.DoubleSpinnerValueFactory vf =
                    (SpinnerValueFactory.DoubleSpinnerValueFactory) spinner.getValueFactory();

            v = Math.max(vf.getMin(), Math.min(vf.getMax(), v));
            vf.setValue(v);
        } catch (NumberFormatException ignored) {

            spinner.getEditor().setText(String.valueOf(spinner.getValue()));
        }
    }



    @FXML
    private void handleExportLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("SVG Vector", "*.svg")
        );
        fileChooser.setInitialFileName("sequence_logo");

        File file = fileChooser.showSaveDialog(logoWebView.getScene().getWindow());
        if (file == null) return;

        String fileName = file.getName().toLowerCase();
        try {
            if (fileName.endsWith(".png")) {
                WritableImage image = logoWebView.snapshot(null, null);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                if (mainController != null) {
                    mainController.setStatus("Logo exported to PNG: " + file.getName());
                }
            } else if (fileName.endsWith(".svg")) {
                if (zScoreMatrix == null) {
                    System.err.println("Z-score matrix not set!");
                    return;
                }
                String svgContent = generateSequenceLogoSVG(zScoreMatrix, heightPerZ);
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.write(svgContent);
                }
                if (mainController != null) {
                    mainController.setStatus("Logo exported to SVG: " + file.getName());
                }
            } else {

                File svgFile = new File(file.getAbsolutePath() + ".svg");
                String svgContent = generateSequenceLogoSVG(zScoreMatrix, heightPerZ);
                try (PrintWriter writer = new PrintWriter(svgFile)) {
                    writer.write(svgContent);
                }
                if (mainController != null) {
                    mainController.setStatus("Logo exported to SVG (default): " + svgFile.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mainController != null) {
                mainController.setStatus("Export failed: " + e.getMessage());
            }
        }
        handleUpdateLogo();
    }


    @FXML
    private void handleFontStyleChange() {
        boolean bold = boldCheckbox.isSelected();
        AAGlyphLibrary.setThinMode(!bold);
        updateLogo();
    }

    @FXML
    private void handleCustomizeColors() {
        Dialog<Map<Character, Color>> dialog = new Dialog<>();
        dialog.setTitle("Customize Amino Acid Colors");

        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));

        Map<Character, ColorPicker> pickers = new HashMap<>();
        String aminoAcids = "ACDEFGHIKLMNPQRSTVWYX";

        int row = 0;
        for (char aa : aminoAcids.toCharArray()) {
            Label label = new Label(String.valueOf(aa));
            Color current = customColorMap.getOrDefault(aa, getDefaultColorForAA(aa));
            ColorPicker colorPicker = new ColorPicker(current);
            pickers.put(aa, colorPicker);

            grid.add(label, 0, row);
            grid.add(colorPicker, 1, row);
            row++;
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                Map<Character, Color> selected = new HashMap<>();
                for (Map.Entry<Character, ColorPicker> entry : pickers.entrySet()) {
                    selected.put(entry.getKey(), entry.getValue().getValue());
                }
                return selected;
            }
            return null;
        });

        Optional<Map<Character, Color>> result = dialog.showAndWait();
        result.ifPresent(selectedColors -> {
            customColorMap.clear();
            customColorMap.putAll(selectedColors);
            colorSchemeBox.setValue("Custom");
            updateLogo();
        });
    }


    public String generateSequenceLogoSVG(Map<Integer, Map<Character, Double>> zScoreMatrix, double svgWidth) {

        if (zScoreMatrix == null || zScoreMatrix.isEmpty()) {

            return "";
        }
        StringBuilder svg = new StringBuilder();

        double glyphBaseHeight = 73.869;
        double glyphBaseWidth = 100;
        double scaleX = 0.6;
        double heightPerZ = this.heightPerZ;

        double fixedGlyphWidth = glyphBaseWidth * scaleX;



        int minPosition = Collections.min(zScoreMatrix.keySet());
        int maxPosition = Collections.max(zScoreMatrix.keySet());



        int totalPositions = maxPosition - minPosition + 1;
        double spacing = 50;
        double logoWidth = (totalPositions - 1) * spacing;


        int width = (int) (logoWidth + 2 * spacing + fixedGlyphWidth);
        double startX = (width - logoWidth) / 2.0;
        double xLeft = startX - 5;
        double xRight = startX + (totalPositions - 1) * spacing + fixedGlyphWidth / 2.0;

        logoWebView.setPrefWidth(svgWidth);
        logoWebView.setMinWidth(svgWidth);
        logoWebView.setMaxWidth(svgWidth);

        int yAxisX = (int) (startX -5);

        int height = 600;

        double centerY = height / heightY;
        double lowerAxisY = centerY + 20;

        svg.append("<?xml version='1.0'?>\n");
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' ")
                .append("viewBox='0 0 ").append(width).append(" ").append(height).append("' ")
                .append("preserveAspectRatio='xMidYMid meet' width='100%' height='100%'>\n");


        svg.append("<line x1='").append(xLeft).append("' x2='").append(xRight)
                .append("' y1='").append(centerY).append("' y2='").append(centerY)
                .append("' stroke='black' stroke-width='2'/>\n");




        double[] extremes = calculateZScoreExtremesPerColumn(zScoreMatrix);
        double minSum = extremes[0];
        double maxSum = extremes[1];


        double limitZ = heightPerZ * 1.8;

        double yAxisTop = centerY - maxSum * heightPerZ - (0.3 * (maxSum * heightPerZ));
        double yAxisBottom = centerY - minSum * heightPerZ + (0.3 * (maxSum * heightPerZ));






        svg.append("<line x1='").append(yAxisX).append("' x2='").append(yAxisX)
                .append("' y1='").append(yAxisTop).append("' y2='").append(yAxisBottom)
                .append("' stroke='black' stroke-width='2'/>\n");

        svg.append("<line x1='").append(yAxisX - 5).append("' y1='").append(yAxisTop + 5)
                .append("' x2='").append(yAxisX).append("' y2='").append(yAxisTop)
                .append("' stroke='black' stroke-width='2'/>\n");
        svg.append("<line x1='").append(yAxisX + 5).append("' y1='").append(yAxisTop + 5)
                .append("' x2='").append(yAxisX).append("' y2='").append(yAxisTop)
                .append("' stroke='black' stroke-width='2'/>\n");
        svg.append("<line x1='").append(yAxisX - 5).append("' y1='").append(yAxisBottom - 5)
                .append("' x2='").append(yAxisX).append("' y2='").append(yAxisBottom)
                .append("' stroke='black' stroke-width='2'/>\n");
        svg.append("<line x1='").append(yAxisX + 5).append("' y1='").append(yAxisBottom - 5)
                .append("' x2='").append(yAxisX).append("' y2='").append(yAxisBottom)
                .append("' stroke='black' stroke-width='2'/>\n");


        double yLabelX = yAxisX - 10;
        double yLabelY = (yAxisTop + yAxisBottom) / 2.0;
        svg.append("<text x='").append(yLabelX)
                .append("' y='").append(yLabelY)
                .append("' text-anchor='middle' font-family='sans-serif' font-size='14' transform='rotate(-90 ")
                .append(yLabelX).append(" ").append(yLabelY).append(")'>z-score</text>\n");



        double[] ticks = { maxSum, maxSum / 2.0, minSum / 2.0, minSum };
        for (double z : ticks) {
            double y = centerY - z * heightPerZ;
            svg.append("<line x1='").append(yAxisX - 5).append("' x2='").append(yAxisX + 5)
                    .append("' y1='").append(y).append("' y2='").append(y)
                    .append("' stroke='black' stroke-width='1'/>\n");
            svg.append("<text x='").append(yAxisX - 10)
                    .append("' y='").append(y + 5)
                    .append("' text-anchor='end' font-family='sans-serif' font-size='12'>")
                    .append(String.format(Locale.US, "%.1f", z)).append("</text>\n");
        }


        List<String> positionLabels = new ArrayList<>();
        for (int pos = minPosition; pos <= maxPosition; pos++) {
            if (pos < 0) positionLabels.add("P" + (-pos+1));
            else if (pos == 0) positionLabels.add("P1");
            else positionLabels.add("P" + pos + "’");
        }


        for (int i = 0; i < positionLabels.size(); i++) {
            int pos = minPosition + i;


            Map<Character, Double> column = zScoreMatrix.get(pos);
            if (column == null) continue;

            String labelDigits = positionLabels.get(i);
            boolean addPrime = pos > 0;

            double labelYOffset = centerY + 4;
            double scaleYLabel = 12.0 / glyphBaseHeight;
            double scaleXLabel = scaleX * 0.3;
            double xOffset = startX + i * spacing;
            double labelXOffset = xOffset;


            double labelUnit = fixedGlyphWidth * scaleXLabel / scaleX;

            for (int c = 0; c < labelDigits.length(); c++) {
                char ch = labelDigits.charAt(c);
                String glyph = AAGlyphLibrary.getLabelGlyphPath(ch);
                if (glyph != null) {
                    svg.append("<g transform='translate(").append(labelXOffset).append(",").append(labelYOffset)
                            .append(") scale(").append(scaleXLabel).append(",").append(scaleYLabel).append(")'>\n");
                    svg.append("<path d='").append(glyph).append("' fill='black'/>\n</g>\n");

                    if (ch == 'P') {
                        labelXOffset += labelUnit * 0.7;
                    } else if (ch == '′') {
                        labelXOffset += labelUnit * 0.6;
                    } else {
                        labelXOffset += labelUnit * 0.5;
                    }
                }
            }


            if (addPrime) {
                String glyphPrime = AAGlyphLibrary.getLabelGlyphPath('′');
                if (glyphPrime != null) {
                    svg.append("<g transform='translate(").append(labelXOffset).append(",").append(labelYOffset)
                            .append(") scale(").append(scaleXLabel).append(",").append(scaleYLabel).append(")'>\n");
                    svg.append("<path d='").append(glyphPrime).append("' fill='black'/>\n</g>\n");
                }
            }


            double yCursorPos = centerY;
            double yCursorNeg = centerY;
            List<Map.Entry<Character, Double>> sorted = column.entrySet().stream()
                    .sorted(Comparator.comparingDouble(e -> Math.abs(e.getValue())))
                    .collect(Collectors.toList());
            for (Map.Entry<Character, Double> entry : sorted) {
                char aa = entry.getKey();
                double z = entry.getValue();
                if (z <= 0) continue;

                String path = AAGlyphLibrary.getGlyphPath(aa);
                if (path == null) continue;

                String color = toHexColor(getColorForAA(aa));
                double heightPx = Math.abs(z) * heightPerZ;
                double scaleY = heightPx / glyphBaseHeight;
                double y = (z >= 0) ? yCursorPos - heightPx : yCursorNeg;


                svg.append("<g transform='translate(").append(xOffset).append(",")
                        .append(y).append(") scale(").append(scaleX).append(",").append(scaleY).append(")'>\n");
                svg.append("<path d='").append(path).append("' fill='").append(color).append("'/>\n</g>\n");

                if (z >= 0) {
                    yCursorPos -= heightPx;
                } else {
                    yCursorNeg += heightPx;
                }
            }




            double yCursorBelow = lowerAxisY + 2;
            for (Map.Entry<Character, Double> entry : sorted) {
                char aa = entry.getKey();
                double z = entry.getValue();
                if (z >= 0) continue;

                String path = AAGlyphLibrary.getGlyphPath(aa);
                if (path == null) continue;

                String color = toHexColor(getColorForAA(aa));
                double heightPx = Math.abs(z) * heightPerZ;
                double scaleY = heightPx / glyphBaseHeight;
                double y = yCursorBelow;

                svg.append("<g transform='translate(").append(xOffset).append(",")
                        .append(y).append(") scale(").append(scaleX).append(",").append(scaleY).append(")'>\n");
                svg.append("<path d='").append(path).append("' fill='").append(color).append("'/>\n</g>\n");

                yCursorBelow += heightPx;

            }
        }


        svg.append("<line x1='").append(xLeft).append("' x2='").append(xRight)
                .append("' y1='").append(lowerAxisY).append("' y2='").append(lowerAxisY)
                .append("' stroke='black' stroke-width='2'/>\n");



        if (showLegendCheckbox.isSelected()) {
            svg.append(getColorLegendSVG(colorSchemeBox.getValue(), svgWidth, height));
        }
        svg.append("</svg>");
        return svg.toString();

    }


    private Color getColorForAA(char aa) {
        String selectedScheme = colorSchemeBox.getValue();
        return switch (selectedScheme) {
            case "Grayscale" -> Color.GRAY;
            case "Hydrophobicity" -> switch (aa) {
                case 'A', 'V', 'I', 'L', 'M', 'F', 'W' -> Color.GOLD;
                case 'R', 'K', 'D', 'E' -> Color.LIGHTBLUE;
                case 'S', 'T', 'N', 'Q' -> Color.LIGHTGREEN;
                default -> Color.DARKGRAY;
            };
            case "Random" -> Color.color(Math.random(), Math.random(), Math.random());
            case "Group-based" -> switch (aa) {
                case 'A', 'V', 'L', 'I', 'M' -> Color.GREEN;
                case 'F', 'Y', 'W' -> Color.ORANGE;
                case 'S', 'T', 'N', 'Q' -> Color.PINK;
                case 'K', 'R', 'H' -> Color.BLUE;
                case 'D', 'E' -> Color.RED;
                case 'C' -> Color.GOLD;
                case 'G' -> Color.GRAY;
                case 'P' -> Color.TURQUOISE;
                case 'X' -> Color.BLACK;
                default -> Color.DARKGRAY;
            };
            default -> switch (aa) {
                case 'A' -> Color.DARKORANGE;
                case 'R' -> Color.DARKBLUE;
                case 'N' -> Color.MEDIUMPURPLE;
                case 'D' -> Color.FIREBRICK;
                case 'C' -> Color.DARKGREEN;
                case 'Q' -> Color.CORNFLOWERBLUE;
                case 'E' -> Color.TOMATO;
                case 'G' -> Color.GREY;
                case 'H' -> Color.INDIGO;
                case 'I' -> Color.GOLD;
                case 'L' -> Color.KHAKI;
                case 'K' -> Color.SLATEBLUE;
                case 'M' -> Color.SANDYBROWN;
                case 'F' -> Color.HOTPINK;
                case 'P' -> Color.LIGHTSEAGREEN;
                case 'S' -> Color.CORAL;
                case 'T' -> Color.MEDIUMVIOLETRED;
                case 'W' -> Color.DARKMAGENTA;
                case 'Y' -> Color.DARKGOLDENROD;
                case 'V' -> Color.OLIVE;
                case 'X' -> Color.LIGHTGRAY;
                default -> Color.BLACK;
            };
            case "Custom" -> customColorMap.getOrDefault(aa, getDefaultColorForAA(aa));
        };
    }

    private Color getDefaultColorForAA(char aa) {
        return switch (aa) {
            case 'A' -> Color.DARKORANGE;
            case 'R' -> Color.DARKBLUE;
            case 'N' -> Color.MEDIUMPURPLE;
            case 'D' -> Color.FIREBRICK;
            case 'C' -> Color.DARKGREEN;
            case 'Q' -> Color.CORNFLOWERBLUE;
            case 'E' -> Color.TOMATO;
            case 'G' -> Color.GREY;
            case 'H' -> Color.INDIGO;
            case 'I' -> Color.GOLD;
            case 'L' -> Color.KHAKI;
            case 'K' -> Color.SLATEBLUE;
            case 'M' -> Color.SANDYBROWN;
            case 'F' -> Color.HOTPINK;
            case 'P' -> Color.LIGHTSEAGREEN;
            case 'S' -> Color.CORAL;
            case 'T' -> Color.MEDIUMVIOLETRED;
            case 'W' -> Color.DARKMAGENTA;
            case 'Y' -> Color.DARKGOLDENROD;
            case 'V' -> Color.OLIVE;
            case 'X' -> Color.LIGHTGRAY;
            default -> Color.BLACK;
        };
    }

    public class ColorPickerDialog {
        public static Color show(String title, Color initial) {
            ColorPicker picker = new ColorPicker(initial);
            Dialog<Color> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.getDialogPane().setContent(picker);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(button -> button == ButtonType.OK ? picker.getValue() : null);
            return dialog.showAndWait().orElse(null);
        }
    }



    private String getColorLegendSVG(String selectedScheme, double svgWidth, double svgHeight) {
        Map<String, Color> legend = new LinkedHashMap<>();
        switch (selectedScheme) {
            case "Hydrophobicity" -> {
                legend.put("Hydrophobic", Color.GOLD);
                legend.put("Charged", Color.LIGHTBLUE);
                legend.put("Polar", Color.LIGHTGREEN);
                legend.put("Other", Color.DARKGRAY);
            }
            case "Group-based" -> {
                legend.put("Aliphatic", Color.GREEN);
                legend.put("Aromatic", Color.ORANGE);
                legend.put("Polar uncharged", Color.PINK);
                legend.put("Basic", Color.BLUE);
                legend.put("Acidic", Color.RED);
                legend.put("Cysteine", Color.GOLD);
                legend.put("Glycine", Color.GRAY);
                legend.put("Proline", Color.TURQUOISE);
                legend.put("Unknown (X)", Color.BLACK);
            }
            case "Grayscale", "Random" -> {
                legend.put("Not applicable", Color.GRAY);
            }
            default -> {
                legend.put("Individual AA colors", Color.DARKGRAY);
            }
        }

        StringBuilder legendSVG = new StringBuilder();
        double paddingFromBottom = 20;
        double baseY = svgHeight - paddingFromBottom;
        double rowHeight = 20;
        int maxRowWidth = (int) svgWidth - 40;
        double x = 20;
        int currentRowWidth = 0;
        int rowCount = 0;

        double lineHeight = 18;
        double paddingRight = 20;

        legendSVG.append("<g font-family='Arial' font-size='12'>\n");

        for (Map.Entry<String, Color> entry : legend.entrySet()) {
            String label = entry.getKey();
            Color fxColor = entry.getValue();
            String hex = String.format("#%02X%02X%02X",
                    (int) (fxColor.getRed() * 255),
                    (int) (fxColor.getGreen() * 255),
                    (int) (fxColor.getBlue() * 255));

            double labelWidth = label.length() * 7 + 30;
            if (currentRowWidth + labelWidth > maxRowWidth) {
                rowCount++;
                x = 20;
                currentRowWidth = 0;
            }

            double y = baseY - rowHeight * rowCount;

            legendSVG.append("<rect x='").append(x).append("' y='").append(y)
                    .append("' width='12' height='12' fill='").append(hex).append("'/>\n");
            legendSVG.append("<text x='").append(x + 16).append("' y='").append(y + 11)
                    .append("'>").append(label).append("</text>\n");

            x += labelWidth;
            currentRowWidth += labelWidth;
        }
        legendSVG.append("</g>\n");
        return legendSVG.toString();
    }



    private String toHexColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    public void setSorting(String sorting) {
        this.sorting = sorting;
    }

    public void setShowX(boolean showX) {
        this.showX = showX;
    }

    private List<Character> getSortedAminoAcids() {
        String defaultSet = showX ? "AVLIMPFYWGSTCQNKRHEDX" : "AVLIMPFYWGSTCQNKRHED";
        return switch (sorting) {
            case "Alphabetical" -> {
                List<Character> list = new ArrayList<>();
                for (char c : defaultSet.toCharArray()) list.add(c);
                Collections.sort(list);
                yield list;
            }
            case "Hydrophobicity" -> stringToCharList("IVLFCMAWGTSYPHNDQEKRX");
            case "Polarity" -> stringToCharList("GAVLIMFWPSTCYNQDEKHRX");
            default -> stringToCharList(defaultSet);
        };
    }

    private List<Character> stringToCharList(String s) {
        List<Character> list = new ArrayList<>();
        for (char c : s.toCharArray()) {
            if (!showX && c == 'X') continue;
            list.add(c);
        }
        return list;
    }
}
