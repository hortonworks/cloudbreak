package com.sequenceiq.periscope.rest.json;

public class ClusterJson implements Json {

    private long id;
    private String host;
    private String port;
    private String state;
    private String appMovement;

    public ClusterJson() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAppMovement() {
        return appMovement;
    }

    public void setAppMovement(String appMovement) {
        this.appMovement = appMovement;
    }

}
