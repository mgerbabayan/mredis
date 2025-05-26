package com.mg.redism.tools;

import com.mg.redism.to.RNode;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by mger on 01.09.2019.
 */
public class GUIUtil {


    public synchronized static TreeItem<RNode> fromRNodeToGUI(LinkedHashMap<String, RNode> nodeList, TreeItem<RNode> root) {

            if (nodeList != null) {
                for (RNode rn : nodeList.values()) {
                    TreeItem tr = new RTreeItem(rn);

                    root.getChildren().add(tr);
                    fromRNodeToGUI(rn.getChildren(), tr);
                }
            }
            return root;

    }


    public static class RTreeItem extends TreeItem<RNode> {
        public RTreeItem(RNode rNode) {
            super(rNode);


        }


    }

}
