package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.common.api.tag.model.Tags;

public class DatabaseStack {

    private final Network network;

    private final DatabaseServer databaseServer;

    private final String template;

    private final Tags tags;

    public DatabaseStack(Network network, DatabaseServer databaseServer, Tags tags, String template) {
        this.network = network;
        this.databaseServer = databaseServer;
        this.tags = new Tags(tags);
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

    public Tags getTags() {
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
