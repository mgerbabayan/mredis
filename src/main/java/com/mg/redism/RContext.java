package com.mg.redism;

import com.mg.redism.to.RConnection;
import com.mg.redism.tools.BinaryJedisClient;
import com.mg.redism.tools.KeysUtil;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by mger on 02.09.2019.
 */
public class RContext {
    private static RContext context;

    private Stage primaryStage;
    private RConnection connection;
    private BinaryJedisClient jedis;
    private boolean needToUpdate = true;

    private RContext(BinaryJedisClient jedis, RConnection connection) {
        this.jedis = jedis;
        this.connection = connection;
    }


    public static void reInitContext(RConnection connection) throws IOException {
        //reseting context
        if (context != null) {
            //destroying context
            context.close();
        }

        BinaryJedisClient binaryJedisClient = KeysUtil.connect(connection.getServer(), connection.getPass(), 15000, connection.isSsl());

        binaryJedisClient.auth(connection.getPass());

        RContext context = new RContext(binaryJedisClient, connection);
        context.setPrimaryStage(RContext.context.getPrimaryStage());
        RContext.context = context;
    }

    public static RContext getContext() {
        if (context == null) {
            context = new RContext(null, null);
        }
        return context;
    }

    private void close() throws IOException {
        if (jedis != null) {
            jedis.close();
        }
    }

    public BinaryJedisClient getJedis() {
        return jedis;
    }

    public RConnection getConnection() {
        return connection;
    }

    public boolean isNeedToUpdate() {
        return needToUpdate;
    }

    public void setNeedToUpdate(boolean needToUpdate) {
        this.needToUpdate = needToUpdate;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
