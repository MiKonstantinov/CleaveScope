package org.example;


import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;
import java.io.IOException;
import java.io.PrintWriter;
import javafx.application.Platform;

public class AnalyzeTabController {

    @FXML
    private TextArea outputArea;
    @FXML
    private TextArea alignedOutputArea;

    @FXML
    private TextField beforeSiteField;

    @FXML
    private TextField afterSiteField;
    @FXML
    private CheckBox excludeInitialMethionineCheckBox;


    private MainController mainController;
    private File lastDirectory;
    private List<File> selectedFiles = new ArrayList<>();


    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleCopyToClipboard() {
        String content = alignedOutputArea.getText();
        if (content != null && !content.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
            clipboardContent.putString(content);
            clipboard.setContent(clipboardContent);
        }
    }

    @FXML
    private void handleSaveToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Aligned Peptides");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialFileName("aligned_peptides.txt");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(alignedOutputArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mainController != null) {
            System.out.println("Updating status...");
            mainController.setStatus("File saved: " + file.getName());
        } else {
            System.out.println("mainController == null");
        }
    }

    private Map<Character, Integer> totalLetterCount = new HashMap<>();
    private Map<Integer, Map<Character, Integer>> lastPositionLetterCounts = new HashMap<>();


    @FXML
    public void handleSelectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Mascot XML Files");



        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Mascot XML", "*.xml")
        );
        if (lastDirectory != null && lastDirectory.exists()) {
            fileChooser.setInitialDirectory(lastDirectory);
        }

        List<File> files = fileChooser.showOpenMultipleDialog(outputArea.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            selectedFiles = files;
            lastDirectory = files.get(0).getParentFile();

            if (mainController != null) {
                mainController.setStatus("Selected " + files.size() + " files.");
            }
        } else {
            outputArea.appendText("No files selected.\n");
            selectedFiles = new ArrayList<>();
        }
    }

    @FXML
    public void handleProcessFiles() {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            outputArea.appendText("No files selected! Please select files first.\n");
            if (mainController != null) {
                mainController.setStatus("No files selected!");
            }
            return;
        }
        Platform.runLater(() -> {
            if (mainController != null) mainController.setStatus("Processing files...");
            processFiles(selectedFiles);
        });
    }

    private List<String> generatePositionLabels(int left, int right) {
        List<String> labels = new ArrayList<>();
        for (int pos = -left + 1; pos <= right; pos++) {
            if (pos < 0) {
                labels.add("P" + (-pos+1));
            } else if (pos == 0) {
                labels.add("P1");
            } else {
                labels.add("P" + pos + "′");
            }
        }
        return labels;
    }

    private void processFiles(List<File> files) {
        outputArea.clear();
        int left = parseOrDefault(beforeSiteField.getText(), 6);
        int right = parseOrDefault(afterSiteField.getText(), 5);


        left = Math.max(1, left);
        int maxLength = 12;
        if (left > maxLength) left = maxLength;
        if (right > maxLength) right = maxLength;

        Map<Integer, Map<Character, Integer>> positionLetterCounts = new HashMap<>();
        for (int k = -(left - 1); k <= right; k++) {
            positionLetterCounts.put(k, new HashMap<>());
        }


        totalLetterCount.clear();
        List<String> alignedFragments = new ArrayList<>();

        for (File file : files) {
            outputArea.appendText("\nProcessed file: " + file.getName() + "\n");
            processXMLFile(file, positionLetterCounts, totalLetterCount, alignedFragments, left, right);;
        }

        List<String> positionLabels = generatePositionLabels(left, right);
        outputArea.appendText("\nSummed amino acids occurrences at positions from " +
                positionLabels.get(0) + " to " + positionLabels.get(positionLabels.size() - 1) + " across all files:\n");
        outputArea.appendText("AA " + String.join(" ", positionLabels) + "\n");
        for (char letter : "ARNDCEQGHILKMFPSTWYVX".toCharArray()) {
            boolean letterExists = false;
            StringBuilder countsPerPosition = new StringBuilder();
            for (int pos = -(left - 1); pos <= right; pos++) {
                int count = positionLetterCounts.get(pos).getOrDefault(letter, 0);
                if (count > 0) {
                    letterExists = true;
                }
                countsPerPosition.append(count).append(", ");
            }

            if (letterExists) {
                countsPerPosition.setLength(countsPerPosition.length() - 2);
                outputArea.appendText(letter + " " + countsPerPosition + "\n");
            }
        }

        outputArea.appendText("\nTotal amino acids occurrences in all protein sequences:\n");
        totalLetterCount.forEach((letter, count) -> outputArea.appendText(letter + " " + count + "\n"));

        if (positionLetterCounts.containsKey(0)) {
            Map<Character, Integer> p1Counts = positionLetterCounts.get(0);

            int totalCleavageSites = p1Counts.entrySet().stream()
                    .filter(e -> e.getKey() != 'Z')
                    .filter(e -> e.getKey() != 'X')
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            outputArea.appendText("\nTotal cleavage sites (P1): " + totalCleavageSites + "\n");
        }


        alignedOutputArea.clear();
        StringBuilder alignedBuffer = new StringBuilder();
        for (String fragment : alignedFragments) {

            alignedBuffer.append(fragment).append("\n");
        }
        alignedOutputArea.setText(alignedBuffer.toString());


        lastPositionLetterCounts = positionLetterCounts;
        updateHeatmap(positionLetterCounts);
        if (mainController != null) {
            mainController.setStatus("Processing complete.");
        }
        if (mainController != null) {
            HeatmapTabController heatmap = mainController.getHeatmapTabController();
            if (heatmap != null) {
                heatmap.setData(positionLetterCounts, totalLetterCount);
            }
        }

    }

    private int parseOrDefault(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    private void processXMLFile(File xmlFile,
                                Map<Integer, Map<Character, Integer>> positionLetterCounts,
                                Map<Character, Integer> totalLetterCount,
                                List<String> alignedFragments,
                                int left,
                                int right) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            NodeList hitList = document.getElementsByTagName("hit");
            outputArea.appendText("Number of protein hits: " + hitList.getLength() + "\n");

            for (int i = 0; i < hitList.getLength(); i++) {
                Node hitNode = hitList.item(i);
                if (hitNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element hitElement = (Element) hitNode;

                    String largeSequence = getTagValue("prot_seq", hitElement);
                    if (largeSequence == null || largeSequence.isEmpty()) {
                        outputArea.setStyle("-fx-text-fill: red;");
                        outputArea.appendText("[ERROR] No Protein Sequence!!!\n" + "Please check Mascot export settings\n");
                        outputArea.setStyle("-fx-text-fill: black;");
                        return;
                    }

                    for (char c : largeSequence.toCharArray()) {
                        char aa = isStandardAminoAcid(c) ? c : 'X';
                        totalLetterCount.put(aa, totalLetterCount.getOrDefault(aa, 0) + 1);
                    }

                    NodeList peptideNodes = hitElement.getElementsByTagName("peptide");
                    Set<Integer> seenPositions = new HashSet<>();

                    for (int j = 0; j < peptideNodes.getLength(); j++) {
                        Node peptideNode = peptideNodes.item(j);
                        if (peptideNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element peptideElement = (Element) peptideNode;
                            String smallSequence = getTagValue("pep_seq", peptideElement);
                            String beforeChar = getTagValue("pep_res_before", peptideElement);
                            String afterChar = getTagValue("pep_res_after", peptideElement);

                            int startPosition = largeSequence.indexOf(smallSequence);
                            while (startPosition >= 0) {
                                boolean excludeInitialM = excludeInitialMethionineCheckBox.isSelected();
                                if (!beforeChar.isEmpty()) {
                                    int position = startPosition - 1;


                                    boolean isInitialMethionine = excludeInitialM &&
                                            beforeChar.equals("M") &&
                                            position == 0;

                                    if (!isInitialMethionine && position >= 0 && position < largeSequence.length() && seenPositions.add(position)) {
                                        countNextLetter(position, largeSequence, positionLetterCounts, left, right);
                                        alignedFragments.add(extractAlignedFragment(largeSequence, position, left, right));
                                    }
                                }

                                if (!afterChar.isEmpty()) {
                                    int position = startPosition + smallSequence.length() - 1;
                                    if (position + 1 < largeSequence.length() && seenPositions.add(position)) {
                                        countNextLetter(position, largeSequence, positionLetterCounts, left, right);
                                        alignedFragments.add(extractAlignedFragment(largeSequence, position, left, right));
                                    }
                                }

                                startPosition = largeSequence.indexOf(smallSequence, startPosition + 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            outputArea.appendText("Error processing file " + xmlFile.getName() + ": " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void countNextLetter(int position, String sequence,
                                 Map<Integer, Map<Character, Integer>> counts,
                                 int left, int right) {
        for (int offset = -(left - 1); offset <= right; offset++) {
            int index = position + offset;
            char c = (index >= 0 && index < sequence.length()) ? sequence.charAt(index) : 'Z';
            char aa = (c == 'Z') ? 'Z' : (isStandardAminoAcid(c) ? c : 'X');


            counts.computeIfAbsent(offset, k -> new HashMap<>());
            Map<Character, Integer> aaMap = counts.get(offset);
            aaMap.put(aa, aaMap.getOrDefault(aa, 0) + 1);
        }
    }


    private String extractAlignedFragment(String sequence, int cleavageIndex, int left, int right) {
        StringBuilder fragment = new StringBuilder();
        for (int offset = -(left - 1); offset <= right; offset++) {
            int index = cleavageIndex + offset;
            char c = (index >= 0 && index < sequence.length()) ? sequence.charAt(index) : 'Z';
            fragment.append(c);
        }
        return fragment.toString();
    }


    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node != null ? node.getTextContent().trim() : null;
        }
        return null;
    }

    private boolean isStandardAminoAcid(char c) {
        return "ARNDCEQGHILKMFPSTWYV".indexOf(c) >= 0;
    }


    private void updateHeatmap(Map<Integer, Map<Character, Integer>> positionLetterCounts) {

    }
}

