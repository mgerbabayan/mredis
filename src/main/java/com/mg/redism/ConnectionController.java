package com.mg.redism;

import com.mg.redism.to.RConnection;
import com.mg.redism.tools.BinaryJedisClient;
import com.mg.redism.tools.ConfigUtil;
import com.mg.redism.tools.EncryptUtil;
import com.mg.redism.tools.KeysUtil;
import com.sun.javafx.collections.ObservableSequentialListWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by mger on 01.09.2019.
 */
public class ConnectionController {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

    @FXML
    private Label connVersion;

    @FXML
    private AnchorPane connectionDialog;

    @FXML
    private ListView<RConnection> connectionList;

    @FXML
    private TextField connName;

    @FXML
    private TextField connServer;

    @FXML
    private PasswordField connAuth;


    @FXML
    private CheckBox sslCheck;

    private EncryptUtil encryptUtil = new EncryptUtil();
    private ConfigUtil configUtil = new ConfigUtil("mredis.conf");

    @FXML
    public void initialize() {

        if (configUtil.configExists()) {
            try {
                List<RConnection> connections = configUtil.loadFile();
                if (connections != null && !connections.isEmpty()) {
                    //filing up connections
                    loadList((List) encryptUtil.decryptRConnections(connections));
                    connectionList.getSelectionModel().select(0);
                    fillFields(connectionList.getSelectionModel().getSelectedItem());
                } else {
                    connServer.setText("localhost");
                }
                //
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Can't load config from file '" + configUtil.getFileName() + "' ! " + e.getMessage());
                //alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(sw.toString())));
                alert.showAndWait();
                connServer.setText("localhost");
            }
        } else {
            connServer.setText("localhost");
        }

        connVersion.setText(Constants.version);
    }

    private void loadList(List<RConnection> connections) {
        connectionList.setItems(new ObservableSequentialListWrapper(connections));
    }

    @FXML
    private void mClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 1) {
                RConnection connection = connectionList.getSelectionModel().getSelectedItem();

                //filling fields
                fillFields(connection);
            }
            if (mouseEvent.getClickCount() == 2) {
                doConnect(null);
            }
        }
    }

    @FXML
    private void doSave(ActionEvent event) {
        RConnection connection = connectionList.getSelectionModel().getSelectedItem();
        if (connection == null) {
            connection = new RConnection();
            //adding connection
            connectionList.getItems().add(0, connection);
            connectionList.getSelectionModel().select(0);
        }
        connection.setName(connName.getText());
        connection.setServer(connServer.getText());
        connection.setPass(connAuth.getText());
        connection.setSsl(sslCheck.isSelected());
        //saving file
        try {
            configUtil.saveFile(new ArrayList<RConnection>(encryptUtil.encryptRConnections(connectionList.getItems())));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            logger.error(e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Can't save config to file '" + configUtil.getFileName() + "' ! " + e.getMessage());
            alert.showAndWait();
        }
        connectionList.refresh();
    }

    @FXML
    private void doCancel(ActionEvent event) {
        Stage connectionsStage = (Stage) connectionDialog.getScene().getWindow();
        connectionsStage.close();
    }

    @FXML
    private void doDelete(ActionEvent event) {
        RConnection connection = connectionList.getSelectionModel().getSelectedItem();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete connection");
        alert.setHeaderText("Look, a Confirmation Dialog");
        alert.setContentText("Are you sure, you want to delete connection " + connection + "?");

        ButtonType buttonYes = new ButtonType("Yes");
        ButtonType buttonNo = new ButtonType("No");
        ButtonType buttonCancel = new ButtonType("Cancel");
        alert.getButtonTypes().setAll(buttonYes, buttonCancel, buttonNo);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonYes) {

            connectionList.getItems().remove(connectionList.getSelectionModel().getSelectedIndex());

            if (!connectionList.getItems().isEmpty()) {
                fillFields(connectionList.getSelectionModel().getSelectedItem());
            } else {
                connServer.setText("localhost");
            }
        }

        //saving file
        try {
            configUtil.saveFile(new ArrayList<RConnection>(encryptUtil.encryptRConnections(connectionList.getItems())));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            logger.error(e.getMessage(), e);
            Alert alertDel = new Alert(Alert.AlertType.ERROR);
            alertDel.setHeaderText("Can't save config to file '" + configUtil.getFileName() + "' ! " + e.getMessage());
            alertDel.showAndWait();
        }
    }

    private void fillFields(RConnection connection) {
        //filling fields
        if (connection != null) {
            connName.setText(connection.getName());
            connServer.setText(connection.getServer());
            connAuth.setText(connection.getPass());
            sslCheck.setSelected(connection.isSsl());
        }
    }

    @FXML
    private void doNew(ActionEvent event) {

        RConnection exist = connectionList.getSelectionModel().getSelectedItem();
        if ((exist != null && exist.getName() != null && exist.getName().equals(connName.getText())) || connName.getText() == null || connName.getText().trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Please change the name !");
            alert.showAndWait();
            return;
        }

        RConnection connection = new RConnection();
        connection.setServer("localhost:6379");

        connection.setName(connName.getText());

        connectionList.getItems().add(0, connection);
        connectionList.getSelectionModel().select(0);

        fillFields(connection);

        connectionList.refresh();
    }

    @FXML
    private void doConnect(ActionEvent event) {
        try {
            RConnection connection = new RConnection();
            connection.setName(connName.getText());
            connection.setServer(connServer.getText());
            //connection.setPort(connPort.getText() == null || connPort.getText().isEmpty() ? 6379 : Integer.parseInt(connPort.getText()));
            connection.setPass(connAuth.getText());
            connection.setSsl(sslCheck.isSelected());

            RContext.reInitContext(connection);
            Stage connectionsStage = (Stage) connectionDialog.getScene().getWindow();
            if (RContext.getContext().getPrimaryStage() != null) {
                RContext.getContext().getPrimaryStage().setTitle(Constants.name + " - " + RContext.getContext().getConnection().getName() + "[" + RContext.getContext().getConnection().getServer() + "] ");
            }
            connectionsStage.close();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Connection failed ! " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void doTest(ActionEvent event) {
        try {
            BinaryJedisClient jedis = KeysUtil.connect(connServer.getText(), connAuth.getText(), 15000, sslCheck.isSelected());
            jedis.auth(connAuth.getText());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Connection " + (jedis.isCluster() ? "to cluster" : "") + " OK!");
            alert.showAndWait();
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Connection failed ! " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
