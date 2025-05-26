package com.mg.redism;

import com.mg.redism.tools.GUIUtil;
import javafx.scene.image.Image;

/**
 * Created by mger on 04.09.2019.
 */
public class Constants {
    public final static String version = "1.4.3b";
    public final static String name = "Redis Graphical Viewer";

    public final static Image mainIcon = new Image(GUIUtil.class.getResourceAsStream("/main_32x32.png"));
    public static final int MAX_PACK = 100000;
    public static final int MAX_CHILDREN = 10000;
}
