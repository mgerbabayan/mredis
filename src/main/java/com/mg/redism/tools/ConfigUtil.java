package com.mg.redism.tools;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mg.redism.to.RConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by mger on 02.09.2019.
 */
public class ConfigUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

    private String fileName = "mredis.conf";

    public ConfigUtil() {
    }

    public ConfigUtil(String fileName) {
        this.fileName = fileName;
    }

    public List<RConnection> loadFile() throws IOException {

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(fileName));
        RConnection[] data = gson.fromJson(reader, RConnection[].class); // contains the whole reviews list
        reader.close();
        return Arrays.asList(data);

    }

    public void saveFile(Collection<RConnection> connections) throws IOException {
        JsonWriter writer = new JsonWriter(new FileWriter(fileName));
        Gson gson = new Gson();

        gson.toJson(connections.toArray(new RConnection[connections.size()]), RConnection[].class, writer);
        writer.close();
    }

    public boolean configExists() {
        return new File(fileName).exists();
    }

    public String getFileName() {
        return fileName;
    }
//    public static void main(String[] args) throws IOException {
//        RConnection rConnection = new RConnection();
//        rConnection.setClassName("test-className");
//        rConnection.setPort(1234);
//        rConnection.setServer("qqq.www.eee.rrr");
//        rConnection.setPass("1qqqwqwqPPP");
//
//        RConnection rConnection2 = new RConnection();
//        rConnection2.setClassName("test-name2");
//        rConnection2.setPort(12234);
//        rConnection2.setServer("qqq2.www.eee.rrr");
//        rConnection2.setPass("1qqqwqwq2PPP");
//
//        RConnection rConnection3 = new RConnection();
//        rConnection3.setClassName("test-name3");
//        rConnection3.setPort(12334);
//        rConnection3.setServer("qqq3.www.eee.rrr");
//        rConnection3.setPass("1qqqwqwq3PPP");
//
//        new ConfigUtil("test.config").saveFile(new EncryptUtil(null).encryptRConnections(Arrays.asList(rConnection, rConnection2, rConnection3)));
//
//        Collection sss = new EncryptUtil(null).decryptRConnections(new ConfigUtil("test.config").loadFile());
//        System.out.println(sss);
//    }
}
