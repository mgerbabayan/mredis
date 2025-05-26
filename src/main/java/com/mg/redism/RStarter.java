package com.mg.redism;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.net.URL;

public class RStarter extends Application {
    private static Logger logger = LoggerFactory.getLogger(RStarter.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        RContext.getContext().setPrimaryStage(primaryStage);
        Stage dialog = new Stage();
        Parent connection = FXMLLoader.load(getClass().getClassLoader().getResource("connections.fxml"));
        dialog.setTitle(Constants.name + " - Connection");
        dialog.setResizable(false);
        dialog.setScene(new Scene(connection, 600, 400));

        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getIcons().add(Constants.mainIcon);
        dialog.showAndWait();

        if (RContext.getContext() == null || RContext.getContext().getJedis() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("No Connection was selected!");
            alert.showAndWait();
            System.exit(0);
        }

        Parent root = FXMLLoader.load(getClass().getResource("details.fxml"));
        primaryStage.setTitle(Constants.name + " - " + RContext.getContext().getConnection().getName() + "[" + RContext.getContext().getConnection().getServer() + "] ");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.getIcons().add(Constants.mainIcon);

        primaryStage.show();
    }


    public static void main(String[] args) {
        PropertyConfigurator.configure(RStarter.class.getClassLoader().getResource("log4j2.xml"));
        BasicConfigurator.configure();
        logger.info("---- Started application ---");
        try {
            com.apple.eawt.Application.getApplication().setDockIconImage(ImageIO.read(new URL(ClassLoader.getSystemClassLoader().getResource("main.png").toExternalForm())));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        launch(args);
        logger.info("---- Shutdown application ---");
    }
}
