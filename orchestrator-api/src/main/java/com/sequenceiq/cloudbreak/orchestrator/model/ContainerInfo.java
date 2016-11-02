package com.sequenceiq.cloudbreak.orchestrator.model;

public class ContainerInfo {

    private String id;

    private String name;

    private String host;

    private String image;

    public ContainerInfo(String id, String name, String host, String image) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getImage() {
        return image;
    }
}
