package org.example;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class TestTabController implements HasMainController {

    @FXML
    private WebView testWebView;

    private MainController mainController;

    @Override
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        drawAllGlyphs();
    }

    public void drawAllGlyphs() {
        StringBuilder svg = new StringBuilder();
        double glyphBaseWidth = 100;
        double glyphBaseHeight = 75.869;
        double scale = 1.0;
        double spacing = 20;
        double margin = 20;

        String letters = "ACDEFGHIKLMNPQRSTVWXYZ";
        String symbols = "1234567890′";

        int width = (int) ((letters.length() + 1) * (glyphBaseWidth * scale + spacing));
        int height = (int) ((glyphBaseHeight * scale + spacing) * 2 + margin * 2);

        svg.append("<?xml version='1.0'?>\n");
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width)
                .append("' height='").append(height).append("'>\n");


        for (int i = 0; i < letters.length(); i++) {
            char aa = letters.charAt(i);
            String path = AAGlyphLibrary.getGlyphPath(aa);
            if (path == null) continue;

            double x = margin + i * (glyphBaseWidth * scale + spacing);
            double y = margin;

            svg.append("<g transform='translate(").append(x).append(",").append(y)
                    .append(") scale(").append(scale).append(")'>\n");
            svg.append("<path d='").append(path).append("' fill='blue'/></g>\n");
        }


        for (int i = 0; i < symbols.length(); i++) {
            char ch = symbols.charAt(i);
            String path = AAGlyphLibrary.getGlyphPath(ch);
            if (path == null) continue;

            double x = margin + i * (glyphBaseWidth * scale + spacing);
            double y = margin + glyphBaseHeight * scale + spacing;

            svg.append("<g transform='translate(").append(x).append(",").append(y)
                    .append(") scale(").append(scale).append(")'>\n");
            svg.append("<path d='").append(path).append("' fill='green'/></g>\n");
        }

        svg.append("</svg>");
        testWebView.getEngine().loadContent(svg.toString(), "image/svg+xml");
    }


}
