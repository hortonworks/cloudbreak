package com.sequenceiq.cloudbreak.orchestrator.yarn.model.core;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Container implements Serializable {

    private String uri;

    private String id;

    private String ip;

    private String hostname;

    private String state;

    private Resource resource;

    private Artifact artifact;

    private boolean privilegedContainer;

    private String launchTime;

    private String bareHost;

    private String componentName;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public boolean isPrivilegedContainer() {
        return privilegedContainer;
    }

    public void setPrivilegedContainer(boolean privilegedContainer) {
        this.privilegedContainer = privilegedContainer;
    }

    @JsonProperty("launch_time")
    public String getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(String launchTime) {
        this.launchTime = launchTime;
    }

    @JsonProperty("bare_host")
    public String getBareHost() {
        return bareHost;
    }

    public void setBareHost(String bareHost) {
        this.bareHost = bareHost;
    }

    @JsonProperty("component_name")
    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
}
