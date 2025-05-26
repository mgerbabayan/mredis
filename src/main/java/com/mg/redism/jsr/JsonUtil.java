package com.mg.redism.jsr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.mg.redism.jsr.ContentType.BLOCKDATA;

public class JsonUtil {

    public static String buildJson(Map contents) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(contents);
    }

    public static String buildJson(List<Content> contents) {
        StringBuilder sb = new StringBuilder();
        if (contents == null) {
            return null;
        }
        if (contents.isEmpty()) {
            return "";
        }

        if (contents.size() > 1) {
            boolean first = true;
            sb.append("[");
            for (Content content : contents) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                if (content instanceof Instance) {
                    sb.append("{");
                    toJson((Instance) content, sb);
                    sb.append("}");
                } else {
                    jsonValue(content, sb);
                }
            }
            sb.append("]");
        }
        if (contents.get(0) instanceof Instance) {
            toJson((Instance) contents.get(0), sb);
        } else {
            jsonValue(contents.get(0), sb);
        }
        System.out.println(sb.toString());
        return makePretty(sb.toString());
    }

    private static String makePretty(String string) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(string);
            return gson.toJson(je);
        } catch (Exception ex) {
            //ignoring
        }
        return string;
    }

    private static Class checkingForStandardClasses(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (Throwable t) {
            //just ignoring
        }
        return null;
    }

    private static void toJson(Instance instance, StringBuilder stringBuilder) {
        if (instance.annotations != null && !instance.annotations.isEmpty()) {
            for (Map.Entry<ClassDescription, List<Content>> entry : instance.annotations.entrySet()) {
                Class clazz = checkingForStandardClasses(entry.getKey().name);

                if (clazz != null && Collection.class.isAssignableFrom(clazz)) {
                    stringBuilder.append("[");
                    hackForArrayList(entry.getValue(), stringBuilder);
                    stringBuilder.append("]");
                    continue;
                }

                if ("java.util.Date".equals(entry.getKey().name)) {
                    hackForDate(entry.getValue(), stringBuilder);
                    continue;
                }

                if (clazz != null && Map.class.isAssignableFrom(clazz)) {
                    stringBuilder.append("{");
                    hackForHashMap(entry.getValue(), stringBuilder);
                    stringBuilder.append("}");
                    continue;
                }
            }
        } else if (instance.fielddata != null) {
            stringBuilder.append("{");
            for (Map.Entry<ClassDescription, Map<Field, Object>> entry : instance.fielddata.entrySet()) {
                if (entry.getValue() != null) {
                    boolean first = true;
                    for (Map.Entry<Field, Object> entryField : entry.getValue().entrySet()) {
                        if (first) {
                            first = false;
                        } else {
                            stringBuilder.append(",");
                        }
                        stringBuilder.append("\"").append(entryField.getKey().name).append("\":");
                        jsonValue(entryField.getValue(), stringBuilder);
                    }
                }
            }
            stringBuilder.append("}");
        }
    }

    private static void hackForArrayList(List<Content> contents, StringBuilder stringBuilder) {
        boolean first = true;
        for (Content content : contents) {
            if (content == null) {
                continue;
            }
            if (content.getType() == BLOCKDATA) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            jsonValue(content, stringBuilder);
        }
    }

    private static void hackForHashMap(List<Content> contents, StringBuilder stringBuilder) {
        boolean first = true;
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            if (content.getType() == BLOCKDATA) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            jsonValue(content, stringBuilder);
            stringBuilder.append(":");
            i++;
            content = contents.get(i);
            jsonValue(content, stringBuilder);
        }
    }

    private static void hackForDate(List<Content> contents, StringBuilder stringBuilder) {
        if (contents == null || contents.isEmpty()) {
            stringBuilder.append("null");
            return;
        }
        stringBuilder.append(new SimpleDateFormat("'\"'yyyy-MM-dd'T'HH:mm:ssSSSZ'\"'").format(new Date(bytesToLong(((BlockData) contents.get(0)).buf))));
    }


    public static void jsonValue(Object instance, StringBuilder stringBuilder) {
        if (!(instance instanceof Content)) {
            stringBuilder.append(instance);
            return;
        }
        if (instance == null) {
            stringBuilder.append("null");
            return;
        }
        if (instance instanceof StringObject) {
            stringBuilder.append("\"").append(((StringObject) instance).value).append("\"");
            return;
        }
        if (instance instanceof EnumObject) {
            if (((EnumObject) instance).value == null) {
                stringBuilder.append("null");
                return;
            }
            stringBuilder.append("\"").append(((EnumObject) instance).value.value).append("\"");
            return;
        }
        if (instance instanceof Instance && "java.util.Arrays$ArrayList".equals(((Instance) instance).classdesc.name)) {

            instance = ((Instance) instance).fielddata.values().iterator().next().values().iterator().next();
        }
        if (instance instanceof ArrayObject) {
            stringBuilder.append("[");
            boolean first = true;
            for (Object obj : ((ArrayObject) instance).data) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(",");
                }
                jsonValue(obj, stringBuilder);
            }
            stringBuilder.append("]");
            return;
        }
        if (Date.class.getCanonicalName().equals(((Instance) instance).classdesc.name)) {
            toJson((Instance) instance, stringBuilder);
        } else {
            //stringBuilder.append("{");
            toJson((Instance) instance, stringBuilder);
            //stringBuilder.append("}");
        }
    }

    public static long bytesToLong(byte... bytes) {
        long i = 0;
        for (byte b : bytes) {
            i = (i << 8) + (b & 0XFF);
        }
        return i;
    }

}
