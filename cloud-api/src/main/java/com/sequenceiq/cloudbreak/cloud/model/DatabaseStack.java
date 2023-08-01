package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

public class DatabaseStack {

    private final Network network;

    private final DatabaseServer databaseServer;

    private final String template;

    private final Map<String, String> tags;

    @JsonCreator
    public DatabaseStack(
            @JsonProperty("network") Network network,
            @JsonProperty("databaseServer") DatabaseServer databaseServer,
            @JsonProperty("tags") Map<String, String> tags,
            @JsonProperty("template") String template) {

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
        return "DatabaseStack{" +
                "network=" + network +
                ", databaseServer=" + databaseServer +
                ", template='" + template + '\'' +
                ", tags=" + tags +
                '}';
    }
}
