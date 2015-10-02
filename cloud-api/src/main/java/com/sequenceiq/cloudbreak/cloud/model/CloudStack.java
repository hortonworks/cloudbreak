package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CloudStack {

    private final List<Group> groups;
    private final Network network;
    private final Security security;
    private final Image image;
    private final String region;
    private final Map<String, String> parameters;

    public CloudStack(List<Group> groups, Network network, Security security, Image image, String region, Map<String, String> parameters) {
        this.groups = groups;
        this.network = network;
        this.security = security;
        this.image = image;
        this.region = region;
        this.parameters = parameters;
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

    public Map<String, String> getParameters() {
        return parameters;
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
