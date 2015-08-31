package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class CloudStack {

    private List<Group> groups;
    private Network network;
    private Security security;
    private Image image;
    private String region;

    public CloudStack(List<Group> groups, Network network, Security security, Image image, String region) {
        this.groups = groups;
        this.network = network;
        this.security = security;
        this.image = image;
        this.region = region;
    }

    public List<Group> getGroups() {
        return new ArrayList<>(groups);
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudStack{");
        sb.append("groups=").append(groups);
        sb.append(", network=").append(network);
        sb.append(", security=").append(security);
        sb.append(", image=").append(image);
        sb.append(", region='").append(region).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
