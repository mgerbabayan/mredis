package com.mg.redism.to;

/**
 * Created by mger on 02.09.2019.
 */
public class RConnection {
    private String name;
    private String server;
    private String pass;

    private boolean ssl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @Override
    public String toString() {
        return (name == null ? "" : name) + " [" + (ssl ? "SSL:" : "") + server + "]";
    }
}
