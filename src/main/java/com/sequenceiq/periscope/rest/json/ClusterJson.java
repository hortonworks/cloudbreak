package com.sequenceiq.periscope.rest.json;

public class ClusterJson implements Json {

    private String id;
    private String host;
    private String port;

    public ClusterJson() {
    }

    public ClusterJson(String id, String host, String port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
}
