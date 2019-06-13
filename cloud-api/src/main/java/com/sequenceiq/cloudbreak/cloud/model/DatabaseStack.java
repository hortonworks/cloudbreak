package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class DatabaseStack {

    private final Network network;

    private final DatabaseServer databaseServer;

    private final String template;

    private final Map<String, String> tags;

    public DatabaseStack(Network network, DatabaseServer databaseServer, Map<String, String> tags, String template) {
        this.network = network;
        this.databaseServer = databaseServer;
        this.tags = ImmutableMap.copyOf(tags);
        this.template = template;
    }

    public Network getNetwork() {
        return network;
    }

    public DatabaseServer getDatabaseServer() {
        return databaseServer;
    }

    public String getTemplate() {
        return template;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatabaseStack{");
        sb.append("network=").append(network);
        sb.append(", databaseServer=").append(databaseServer);
        sb.append('}');
        return sb.toString();
    }

}
