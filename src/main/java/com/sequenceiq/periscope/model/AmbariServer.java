package com.sequenceiq.periscope.model;

public class AmbariServer {

    private final String host;
    private final String port;
    private final String user;
    private final String pass;

    public AmbariServer(String host, String port, String user, String pass) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }
}
