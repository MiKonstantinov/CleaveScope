package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private TabPane tabPane;

    @FXML
    private Label statusLabel;

    @FXML private ProgressBar swissprotProgressBar;

    private HeatmapTabController heatmapTabController;
    private LogoTabController logoTabController;
    private Stage primaryStage;


    public ProgressBar getSwissprotProgressBar() {
        return swissprotProgressBar;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public HeatmapTabController getHeatmapTabController() {
        return heatmapTabController;
    }

    public LogoTabController getLogoTabController() {
        return logoTabController;
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    private Tab alignedTab;

    @FXML
    public void initialize() {
        loadAnalyzeTab("fxml/analyze_tab.fxml", "Data Analysis");
        loadAndTrackTab("fxml/heatmap_tab.fxml", "Heatmap");
        loadAndTrackTab("fxml/logo_tab.fxml", "Sequence Logo");
        loadAndTrackTab("fxml/aligned_tab.fxml", "Aligned Fragments");
        loadAndTrackTab("fxml/test_tab.fxml", "Test");


        List<Tab> tabsToRemove = new ArrayList<>();
        for (Tab tab : tabPane.getTabs()) {
            if ("Aligned Fragments".equals(tab.getText()) || "Test".equals(tab.getText())) {
                tabsToRemove.add(tab);
            }
        }
        tabPane.getTabs().removeAll(tabsToRemove);
    }

    private void loadAnalyzeTab(String fxmlPath, String tabTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(fxmlPath));
            Node tabContent = loader.load();

            AnalyzeTabController controller = loader.getController();
            controller.setMainController(this);

            Tab tab = new Tab(tabTitle, tabContent);
            tab.setClosable(false);
            tabPane.getTabs().add(tab);
        } catch (IOException e) {
            e.printStackTrace();
            addErrorTab(tabTitle);
        }
    }

    private void loadAndTrackTab(String fxmlPath, String tabTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(fxmlPath));
            Node tabContent = loader.load();
            Object controller = loader.getController();


            if (controller instanceof HeatmapTabController) {
                heatmapTabController = (HeatmapTabController) controller;
            } else if (controller instanceof LogoTabController) {
                logoTabController = (LogoTabController) controller;
            }


            if (controller instanceof HasMainController) {
                ((HasMainController) controller).setMainController(this);
            }

            Tab tab = new Tab(tabTitle, tabContent);
            tab.setClosable(false);
            tabPane.getTabs().add(tab);
        } catch (IOException e) {
            e.printStackTrace();
            addErrorTab(tabTitle);
        }
    }

    private void addErrorTab(String title) {
        Tab errorTab = new Tab(title + " (Error)");
        errorTab.setClosable(false);
        tabPane.getTabs().add(errorTab);
    }
}
