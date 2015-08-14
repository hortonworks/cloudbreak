package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

public class CloudStack {

    private List<Group> groups;

    private Network network;

    private Security security;

    private Image image;

    private String region;

    public CloudStack(List<Group> groups, Network network, Security security, Image image) {
        this.groups = groups;
        this.network = network;
        this.security = security;
        this.image = image;
    }

    public CloudStack(List<Group> groups, Network network, Security security, Image image, String region) {
        this.groups = groups;
        this.network = network;
        this.security = security;
        this.image = image;
        this.region = region;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Network getNetwork() {
        return network;
    }

    public Security getSecurity() {
        return security;
    }

    public Image getImage() {
        return image;
    }

    public String getRegion() {
        return region;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudStack{" +
                "groups=" + groups +
                ", network=" + network +
                ", security=" + security +
                ", image=" + image +
                '}';
    }
    //END GENERATED CODE
}
