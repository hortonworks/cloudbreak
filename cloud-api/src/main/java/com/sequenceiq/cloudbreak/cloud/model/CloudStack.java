package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

public class CloudStack {

    private List<Group> groups;

    private Network network;

    private Security security;

    private Image image;

    public CloudStack(List<Group> groups, Network network, Security security, Image image) {
        this.groups = groups;
        this.network = network;
        this.security = security;
        this.image = image;
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

    @Override
    public String toString() {
        return "CloudStack{" +
                "groups=" + groups +
                ", network=" + network +
                ", security=" + security +
                ", image=" + image +
                '}';
    }
}
