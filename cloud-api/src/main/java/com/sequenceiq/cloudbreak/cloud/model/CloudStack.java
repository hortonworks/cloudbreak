package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Class that describes complete structure of infrastructure that needs to be started on the Cloud Provider
 */
public class CloudStack {

    private final List<Group> groups;
    private final Network network;
    private final Security security;
    private final Image image;
    private final Map<String, String> parameters;

    public CloudStack(List<Group> groups, Network network, Security security, Image image, Map<String, String> parameters) {
        this.groups = ImmutableList.copyOf(groups);
        this.network = network;
        this.security = security;
        this.image = image;
        this.parameters = ImmutableMap.copyOf(parameters);
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
        sb.append('}');
        return sb.toString();
    }
}
