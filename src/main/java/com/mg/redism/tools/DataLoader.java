package com.mg.redism.tools;

import com.mg.redism.to.RNode;
import com.mg.redism.tools.GUIUtil;
import com.mg.redism.tools.KeysUtil;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DataLoader extends Task {

    public static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private String search;
    private KeysUtil keysUtil;
    private TreeItem<RNode> r = new TreeItem<>();

    private LinkedHashMap<String, RNode> listKeys = new LinkedHashMap<>();

    public DataLoader(String search, KeysUtil keysUtil) {
        this.search = search;
        this.keysUtil = keysUtil;


    }

    @Override
    protected Object call() throws Exception {
        keysUtil.getKeys(search, this, listKeys);
//        GUIUtil.fromRNodeToGUI(listKeys, r);
        return true;
    }

    public LinkedHashMap<String, RNode> getListKeys() {
        return listKeys;
    }

    public void setProgress(long curent, long max) {
        this.r = new TreeItem<>();
        GUIUtil.fromRNodeToGUI(listKeys, r);
        updateProgress(curent, max);
    }

    public TreeItem<RNode> getR() {
        return r;
    }
}
