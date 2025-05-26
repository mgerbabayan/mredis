package com.mg.redism.to;

/**
 * Created by mger on 01.09.2019.
 */
public class RField {

    private String id;
    private Object data;
    private long ttl;

    public RField(String id, Object data) {
        this.id = id;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return id;
    }
}
