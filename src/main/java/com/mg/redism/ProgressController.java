package com.mg.redism;

import com.mg.redism.tools.DataLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Created by mger on 25.10.2019.
 */
public class ProgressController {

    @FXML
    private AnchorPane progressId;

    @FXML
    private Label progressText;

    @FXML
    private ProgressBar progressBar;

    private DataLoader dataLoader;
    @FXML
    private Button progressCancel;

    @FXML
    private void initialize() {
    }

    public void setDbSize(Long dbSize) {

        if (dbSize != null) {
            progressText.setText("Please wait. Loading  " + dbSize + " records ...");
           // progressBar.setProgress(1);
        }
    }

    @FXML
    private void onCancel() {
        dataLoader.cancel();
        Stage connectionsStage = (Stage) progressId.getScene().getWindow();
        connectionsStage.close();
    }

    public void setDataLoader(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
        progressBar.progressProperty().bind(dataLoader.progressProperty());
    }
}
