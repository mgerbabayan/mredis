package com.mg.redism;

import com.mg.redism.to.RField;
import com.mg.redism.to.RNode;
import com.mg.redism.to.RType;
import com.mg.redism.tools.BinaryJedisClient;
import com.mg.redism.tools.DataLoader;
import com.mg.redism.tools.GUIUtil;
import com.mg.redism.tools.KeysUtil;
import com.sun.javafx.collections.ObservableSequentialListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.mg.redism.Constants.name;
import static com.mg.redism.Constants.version;
import static com.mg.redism.tools.KeysUtil.transform;

/**
 * Created by mger on 01.09.2019.
 */
public class DetailsControler {

    private static final Logger logger = LoggerFactory.getLogger(DetailsControler.class);

    @FXML
    private AnchorPane mainWindow;

    @FXML
    private MenuBar mainMenu;

    @FXML
    private MenuItem closeMenu;

    @FXML
    private TextField sizeArr;

    @FXML
    private TreeView<RNode> mainTree;

    @FXML
    private ListView<RField> detailsList;

    @FXML
    private TextArea detailsText;

    @FXML
    private TextField ttlField;

    @FXML
    private TextField searchField;

    @FXML
    private TextField textPath;

    @FXML
    private Label status;

    @FXML
    private TextField serverNode;

    @FXML
    private ComboBox<ViewMode> dataType;

    ContextMenu contextMenu = new ContextMenu();


    private final static Image listIcon = new Image(GUIUtil.class.getResourceAsStream("/list_20x20.png"));

    private final static Image openIcon = new Image(GUIUtil.class.getResourceAsStream("/open_20x20.png"));

    private final static Image closedIcon = new Image(GUIUtil.class.getResourceAsStream("/cls_20x20.png"));

    private final static Image unknownIcon = new Image(GUIUtil.class.getResourceAsStream("/unknown_20x20.png"));

    private final static Image hashIcon = new Image(GUIUtil.class.getResourceAsStream("/hash_20x20.png"));

    private final static Image stringIcon = new Image(GUIUtil.class.getResourceAsStream("/string_20x20.png"));

    private final static Image zindexIcon = new Image(GUIUtil.class.getResourceAsStream("/zindex_20x20.png"));

    private final static Image blankIcon = new Image(GUIUtil.class.getResourceAsStream("/blank_20x20.png"));

    @FXML
    private void initialize() {

        logger.info("-- init details form");
        status.setText("Ready");
        dataType.setItems(new ObservableSequentialListWrapper<>(Arrays.asList(ViewMode.values())));
        dataType.getSelectionModel().select(ViewMode.Auto);
        connect();
        mainTree.setCellFactory(tv -> new TreeCell<RNode>() {
            @Override
            public void updateItem(RNode item, boolean empty) {
                super.updateItem(item, empty);
                setDisclosureNode(null);
                setDisable(false);
                if (empty) {
                    setText("");
                    setGraphic(null);
                } else {
                    setText(item.toString()); // appropriate text for item
                    if (getTreeItem().isExpanded()) {
                        setGraphic(new ImageView(openIcon));
                    } else {
                        if (!getTreeItem().isLeaf()) {
                            setGraphic(new ImageView(closedIcon));
                        } else {
                            //need to set based on type
                            if (item.getType() != null) {
                                switch (item.getType()) {
                                    case list:
                                        setGraphic(new ImageView(listIcon));
                                        break;
                                    case hash:
                                        setGraphic(new ImageView(hashIcon));
                                        break;
                                    case string:
                                        setGraphic(new ImageView(stringIcon));
                                        break;
                                    case zset:
                                        setGraphic(new ImageView(zindexIcon));
                                        break;
                                    case more:
                                        setGraphic(new ImageView(blankIcon));
                                        setDisable(true);

                                        break;
                                    default:
                                        setGraphic(new ImageView(unknownIcon));
                                }
                            } else {
                                setGraphic(new ImageView(unknownIcon));
                            }
                        }
                    }
                }
            }
        });

        mainTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newVlaue) -> {
            refresh(mainTree, false);
        });

        detailsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newVlaue) -> {
            listChangedFocus();
        });

        mainTree.setRoot(new TreeItem<>());
        mainTree.setShowRoot(false);
        mainTree.refresh();


        MenuItem menuItem1 = new MenuItem("Refresh");
        menuItem1.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {

                refresh(mainTree, true);
            }
        });
        contextMenu.getItems().add(menuItem1);
        //default load
        // loadData();
    }

    private KeysUtil keysUtil;


    private void connect() {
        BinaryJedisClient jedis = RContext.getContext().getJedis();

        if (jedis == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("No Connection set!");
            //alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(sw.toString())));
            alert.showAndWait();
            return;
        }
        keysUtil = new KeysUtil(jedis, RContext.getContext().getConnection());

    }


    private void loadData() {
        String search = searchField.getText();
        search = search == null || search.trim().isEmpty() ? "*" : search;


        mainTree.setRoot(null);
        mainTree.setShowRoot(false);
        DataLoader dataLoader = new DataLoader(search, keysUtil);
        Stage dialogProgress = showProgress((Stage) mainWindow.getScene().getWindow(), dataLoader);

        dataLoader.stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable,
                                Worker.State oldValue, Worker.State newState) {
                synchronized (dataLoader.getListKeys()) {
                    switch (newState) {
                        case CANCELLED:
                        case SUCCEEDED:
                            logger.info("Finished search with state {} --", newState);
                            TreeItem<RNode> r = new TreeItem<>();

                            GUIUtil.fromRNodeToGUI(dataLoader.getListKeys(), r);
                            mainTree.setRoot(r);
                            mainTree.setShowRoot(false);
                            dialogProgress.close();
                            RContext.getContext().setNeedToUpdate(false);
                            if (r.getChildren().isEmpty()) {
                                status.setText("No records found");
                            } else {
                                status.setText(r.getChildren().size() + " keys found");
                            }

                            break;
                        case FAILED:
                            logger.error("Error : Failed to get records : ", ((DataLoader) ((SimpleObjectProperty) observable).getBean()).getException());
                            dialogProgress.close();
                            //((DataLoader)((SimpleObjectProperty)observable).bean).outcome
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setHeaderText("Failed to get records :" + ((DataLoader) ((SimpleObjectProperty) observable).getBean()).getException().getMessage());
                            alert.showAndWait();
                    }
                }
            }
        });

        dataLoader.progressProperty().
                addListener((observable, oldValue, newValue) ->
                {
                    synchronized (dataLoader.getListKeys()) {
                        mainTree.setRoot(dataLoader.getR());
                        mainTree.setShowRoot(false);
                    }
                });
        if (dialogProgress.isShowing()) {
            new Thread(dataLoader).start();
        }

        cleanDetails();
    }


    private void cleanDetails() {
        //resetting details
        detailsList.setItems(null);
        ttlField.setText(null);
        detailsText.setText(null);
        textPath.setText(null);
        sizeArr.setText(null);
        serverNode.setText(null);
    }

    @FXML
    private void dataTypeChange(ActionEvent event) {

        RField rField = detailsList.getSelectionModel().getSelectedItem();
        if (rField != null) {
            if (rField.getData() != null) {
                if (rField.getData() instanceof byte[]) {
                    detailsText.setText(transform((byte[]) rField.getData(), dataType.getSelectionModel().getSelectedItem()));
                } else {
                    detailsText.setText(rField.getData().toString());
                }
            }
        }
        setPathLabel();
    }

    @FXML
    private void treeClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2 || mouseEvent.getClickCount() == 1) {
                refresh((TreeView<RNode>) mouseEvent.getSource(), mouseEvent.getClickCount() == 2);
            }
        }
    }

    private void refresh(TreeView<RNode> tree, boolean refresh) {
        cleanDetails();
        if ((tree).getSelectionModel() == null) {
            return;
        }
        TreeItem<RNode> treeItem = (tree).getSelectionModel().getSelectedItem();
        if (treeItem == null) {
            return;
        }
        RNode rnode = treeItem.getValue();
        if (refresh) {
            keysUtil.getRNodeFields(rnode);
        }
        if (rnode.getTtl() != 0) {
            ttlField.setText(rnode.getTtl() + "");
        }

        if (rnode.getServerNode() != null) {
            serverNode.setText(rnode.getServerNode());
            serverNode.setTooltip(new Tooltip(rnode.getServerNode()));
        }

        if (rnode.getData() != null) {
            sizeArr.setText(String.valueOf(((List) rnode.getData()).size()));
            detailsList.setItems(new ObservableSequentialListWrapper((List) rnode.getData()));
            if (!detailsList.getItems().isEmpty()) {
                detailsList.getSelectionModel().selectFirst();
            }
        }

        if ((rnode.getData() == null || ((List) rnode.getData()).size() == 0) && rnode.getChildren() != null) {
            sizeArr.setText(String.valueOf(rnode.getChildren().size()));
        }
        setPathLabel();
        (tree).refresh();
    }

    @FXML
    private void doClose(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    private void onSearch(ActionEvent event) {
        loadData();
    }

    @FXML
    private void onTreePressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            refresh((TreeView<RNode>) event.getSource(), true);
        }
    }

    @FXML
    private void doConnection(ActionEvent event) {
        try {
            Stage primaryStage = (Stage) mainWindow.getScene().getWindow();

            Stage dialog = new Stage();
            Parent connection = FXMLLoader.load(getClass().getResource("/connections.fxml"));
            dialog.setTitle(name + " - Connection");
            dialog.setResizable(false);
            dialog.setScene(new Scene(connection, 600, 400));

            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.getIcons().add(Constants.mainIcon);
            dialog.showAndWait();
            if (RContext.getContext().isNeedToUpdate()) {
                connect();
                cleanDetails();
                mainTree.setRoot(new TreeItem<>());
                mainTree.setShowRoot(false);
                mainTree.refresh();
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            status.setText("Failed to connect : " + e.getLocalizedMessage());
        }
    }

    private void setPathLabel() {
        if (detailsList.getSelectionModel() == null || mainTree.getSelectionModel() == null || mainTree.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        RField rField = detailsList.getSelectionModel().getSelectedItem();
        RNode rNode = mainTree.getSelectionModel().getSelectedItem().getValue();
        if (rNode != null) {
            //textPath.getParent().requestFocus();
            textPath.setFocusTraversable(false);
            if (rField != null) {
                if (rNode.getType() == RType.list) {
                    textPath.setText(rField.getId());
                    textPath.setPromptText(rField.getId());
                    textPath.setTooltip(new Tooltip(rField.getId()));
                } else {
                    textPath.setText(rNode.getPath() + "[" + rField.getId() + "]");
                    textPath.setPromptText(rNode.getPath() + "[" + rField.getId() + "]");
                    textPath.setTooltip(new Tooltip(rNode.getPath() + "[" + rField.getId() + "]"));
                }

            } else {
                textPath.setText(rNode.getPath());
                textPath.setPromptText(rNode.getPath());
                textPath.setTooltip(new Tooltip(rNode.getPath()));
            }
        }
    }

    @FXML
    private void lstClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 1) {
                listChangedFocus();
            }
        }
    }

    private void listChangedFocus() {
        RField rField = detailsList.getSelectionModel().getSelectedItem();
        if (rField != null) {
            if (rField.getData() != null) {
                if (rField.getData() instanceof byte[]) {
                    detailsText.setText(transform((byte[]) rField.getData(), dataType.getSelectionModel().getSelectedItem()));
                } else {
                    detailsText.setText(rField.getData().toString());
                }
            }
        }
        setPathLabel();
    }

    private Stage showProgress(Stage primaryStage, DataLoader dataLoader) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/progress.fxml"));

            Stage dialog = new Stage();
            // Parent progress = FXMLLoader.load(loader.);
            dialog.setTitle(name + " - " + RContext.getContext().getConnection().getName() + "[" + RContext.getContext().getConnection().getServer() + "] " + version);
            dialog.setResizable(false);

            dialog.setScene(new Scene(loader.load(), 440, 150));
            ProgressController progressController = loader.getController();

            dialog.setOnCloseRequest(e -> {
                System.exit(0);
            });
            progressController.setDataLoader(dataLoader);
            progressController.setDbSize(keysUtil.getDbSize());
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.getIcons().add(Constants.mainIcon);
            dialog.show();


            return dialog;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            status.setText("Failed to get records : " + e.getLocalizedMessage());
        }
        return null;
    }

    @FXML
    private void doConfig() {
        doInfo(keysUtil.getConfig(), "Name", "Value");
    }

    @FXML
    private void doClients() {
        doInfo(keysUtil.getClients(), "Client", "Params");
    }

    @FXML
    private void doInfo() {
        doInfo(keysUtil.getInfo(), "Name", "Value");
    }

    private void doInfo(Map<String, List<String[]>> data, String column1, String column2) {

        try {
            Stage primaryStage = (Stage) mainWindow.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/serverconfig.fxml"));

            Stage dialog = new Stage();
            // Parent progress = FXMLLoader.load(loader.);
            dialog.setTitle(name + " - " + RContext.getContext().getConnection().getName() + "[" + RContext.getContext().getConnection().getServer() + "] config (" + (data == null ? 0 : data.size()) + " items)");
            //dialog.setResizable(false);
            dialog.setScene(new Scene(loader.load(), 500, 600));
            dialog.setMaxWidth(700);
            ServerConfigController configController = loader.getController();

            for (Map.Entry<String, List<String[]>> entry : data.entrySet()) {
                TableView<String[]> configTable = configController.addConfigTable(entry.getValue(), entry.getKey());
                configController.setConfigNames(configTable, column1, column2);
            }
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.getIcons().add(Constants.mainIcon);
            dialog.showAndWait();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            status.setText("Failed to get server info : " + e.getLocalizedMessage());
        }
    }

    @FXML
    private void onAbout() {
        try {
            Stage primaryStage = (Stage) mainWindow.getScene().getWindow();

            Stage dialog = new Stage();
            Parent about = FXMLLoader.load(getClass().getResource("/about.fxml"));
            //Parent about = FXMLLoader.load(getClass().getClassLoader().getResourceAsStream("/about.fxml"));
            dialog.setTitle(name + " - " + version);
            dialog.setResizable(false);
            dialog.setScene(new Scene(about, 300, 150));

            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.getIcons().add(Constants.mainIcon);
            dialog.showAndWait();

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            status.setText("Failed to get about : " + e.getLocalizedMessage());
        }
    }

    @FXML
    private void onContext(ContextMenuEvent event) {
        //System.out.println("context click");
        contextMenu.show(mainTree, event.getScreenX(), event.getScreenY());
    }

    public enum ViewMode {
        Auto, Text, Java
    }
}
