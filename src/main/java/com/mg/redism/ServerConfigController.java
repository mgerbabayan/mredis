package com.mg.redism;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;

import java.util.List;
import java.util.Map;

/**
 * Created by mger on 25.10.2019.
 */
public class ServerConfigController {

    @FXML
    private TabPane configPane;

    //@FXML
    // private TableView<String[]> configTable;

    @FXML
    private void initialize() {

    }

    private void initConfigPane(TableView<String[]> configTable, String title) {

        Tab tab = new Tab();
        tab.setText(title);
        configPane.getTabs().add(tab);

        configTable.getSelectionModel().setCellSelectionEnabled(true);
        configTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        configTable.getColumns().add(new TableColumn<>());
        configTable.getColumns().add(new TableColumn<>());
        configTable.getColumns().get(0).setCellValueFactory(cellData -> (ObservableValue) new SimpleStringProperty(cellData.getValue()[0]));
        configTable.getColumns().get(1).setCellValueFactory(cellData -> (ObservableValue) new SimpleStringProperty(cellData.getValue()[1]));

        tab.setContent(configTable);
    }


    public TableView<String[]> addConfigTable(List<String[]> config, String title) {
        if (config == null) {
            return null;
        }
        TableView<String[]> configTable = new TableView<>();
        initConfigPane(configTable, title);

        for (String[] entry : config) {
            if ("requirepass".equalsIgnoreCase(entry[0]) ||
                    "masterauth".equalsIgnoreCase(entry[0])
            ) {
                entry[1] = "********";
            }

            configTable.getItems().add(entry);
        }
        configTable.refresh();
        return configTable;
    }

    public void setConfigNames(TableView<String[]> configTable, String column1, String column2) {
        configTable.getColumns().get(0).setText(column1);
        configTable.getColumns().get(1).setText(column2);
    }

    @FXML
    public void onKey(final KeyEvent keyEvent) {
        if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY).match(keyEvent)) {
            if (keyEvent.getSource() instanceof TableView) {
                copySelection((TableView<?>) keyEvent.getSource());
                keyEvent.consume();
            }
        }
    }


    public static void copySelection(TableView<?> selected) {

        StringBuilder sb = new StringBuilder();

        ObservableList<TablePosition> lines = selected.getSelectionModel().getSelectedCells();

        int lastLine = -1;

        for (TablePosition position : lines) {

            int row = position.getRow();
            int col = position.getColumn();

            if (lastLine == row) {
                sb.append('\t');
            } else if (lastLine != -1) {
                sb.append('\n');
            }

            String text = "";
            Object observableValue = selected.getColumns().get(col).getCellObservableValue(row);

            if (observableValue == null) {
                text = "";
            } else if (observableValue instanceof StringProperty) {
                text = ((StringProperty) observableValue).get();
            }

            sb.append(text);

            lastLine = row;
        }

        // create clipboard content
        final ClipboardContent copyContent = new ClipboardContent();
        copyContent.putString(sb.toString());

        // set clipboard content
        Clipboard.getSystemClipboard().setContent(copyContent);

    }
}
