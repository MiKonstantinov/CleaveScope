package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.File;
import java.io.PrintWriter;

public class AlignedTabController {

    @FXML
    private TextArea alignedOutputArea;

    @FXML
    private Button copyButton;

    @FXML
    private Button saveButton;

    @FXML
    public void initialize() {
        copyButton.setOnAction(e -> copyToClipboard());
        saveButton.setOnAction(e -> saveToFile());
    }

    private void copyToClipboard() {
        String text = alignedOutputArea.getText();
        if (!text.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        }
    }

    private void saveToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Aligned Fragments");
        fileChooser.setInitialFileName("aligned_fragments.txt");
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter out = new PrintWriter(file)) {
                out.print(alignedOutputArea.getText());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setAlignedFragments(String fragmentsText) {
        alignedOutputArea.setText(fragmentsText);
    }

    public void appendFragment(String fragment) {
        alignedOutputArea.appendText(fragment + "\n");
    }
}
