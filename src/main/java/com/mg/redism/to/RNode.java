package com.mg.redism.to;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by mger on 01.09.2019.
 * <p>
 * Represents Node of redis data
 */
public class RNode<T> {
    protected String serverNode;
    protected String name;
    protected String path;
    protected RNode parent;
    protected RType type;
    protected long ttl;
    protected T data;
    protected long size;
    protected LinkedHashMap<String, RNode> children;


    public RNode(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RNode getParent() {
        return parent;
    }

    public void setParent(RNode parent) {
        this.parent = parent;
    }

    public LinkedHashMap<String, RNode> getChildren() {
        return children;
    }

    public void setChildren(LinkedHashMap<String, RNode> children) {
        this.children = children;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public long getTtl() {
        return ttl;
    }

    public RType getType() {
        return type;
    }

    public void setType(RType type) {
        this.type = type;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getServerNode() {
        return serverNode;
    }

    public void setServerNode(String serverNode) {
        this.serverNode = serverNode;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        if (children == null) {
            if (data != null && (data instanceof Collection)) {
                return name + " (" + ((Collection) data).size() + ")";
            }
            return name + (type == RType.more ? "" : " (?)");
        }
        return name + " (" + size + ")";
    }


}
