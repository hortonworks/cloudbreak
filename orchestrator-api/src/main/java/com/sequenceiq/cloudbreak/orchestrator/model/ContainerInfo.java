package com.sequenceiq.cloudbreak.orchestrator.model;

public class ContainerInfo {

    private String id;
    private String name;
    private String ipAddres;
    private String image;

    public ContainerInfo(String id, String name, String ipAddres, String image) {
        this.id = id;
        this.name = name;
        this.ipAddres = ipAddres;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIpAddres() {
        return ipAddres;
    }

    public String getImage() {
        return image;
    }
}
