package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

public class DatabaseStack {

    private final Network network;

    private final DatabaseServer databaseServer;

    private final String template;

    private final Map<String, String> tags;

    private DeploymentType deploymentType;

    public DatabaseStack(Network network, DatabaseServer databaseServer, Map<String, String> tags, String template) {
        this(network, databaseServer, tags, template, DeploymentType.PROVISION);
    }

    @JsonCreator
    public DatabaseStack(
            @JsonProperty("network") Network network,
            @JsonProperty("databaseServer") DatabaseServer databaseServer,
            @JsonProperty("tags") Map<String, String> tags,
            @JsonProperty("template") String template,
            @JsonProperty("deploymentType") DeploymentType deploymentType) {

        this.network = network;
        this.databaseServer = databaseServer;
        this.tags = ImmutableMap.copyOf(tags);
        this.template = template;
        this.deploymentType = deploymentType;
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

    public DeploymentType getDeploymentType() {
        return deploymentType;
    }

    public void setDeploymentType(DeploymentType deploymentType) {
        this.deploymentType = deploymentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatabaseStack that = (DatabaseStack) o;
        return Objects.equals(network, that.network) &&
                Objects.equals(databaseServer, that.databaseServer) &&
                Objects.equals(template, that.template) &&
                tags.equals(that.tags) &&
                deploymentType == that.deploymentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(network, databaseServer, template, tags, deploymentType);
    }

    @Override
    public String toString() {
        return "DatabaseStack{" +
                "network=" + network +
                ", databaseServer=" + databaseServer +
                ", template='" + template + '\'' +
                ", tags=" + tags +
                ", deploymentType=" + deploymentType +
                '}';
    }
}