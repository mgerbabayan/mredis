package com.mg.redism;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Created by mger on 04.09.2019.
 */
public class AboutController {

    @FXML
    private AnchorPane aboutDialog;

    @FXML
    private Label versionAbout;

    @FXML
    private void initialize() {
        versionAbout.setText(Constants.version + "  Since 2020");
    }

    @FXML
    private void onAboutOk() {
        Stage connectionsStage = (Stage) aboutDialog.getScene().getWindow();
        connectionsStage.close();
    }
}
