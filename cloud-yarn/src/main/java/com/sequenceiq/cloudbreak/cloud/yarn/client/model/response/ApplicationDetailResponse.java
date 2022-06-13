package com.sequenceiq.cloudbreak.cloud.yarn.client.model.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Container;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;

public class ApplicationDetailResponse implements ApplicationResponse {

    private String id;

    private String name;

    private String uri;

    private String lifetime;

    private List<YarnComponent> components;

    private List<Container> containers;

    private String state;

    private int numberOfContainers;

    private int expectedNumberOfContainers;

    private long launchTime;

    private int numberOfRunningContainers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("expected_number_of_containers")
    public int getExpectedNumberOfContainers() {
        return expectedNumberOfContainers;
    }

    public void setExpectedNumberOfContainers(int expectedNumberOfContainers) {
        this.expectedNumberOfContainers = expectedNumberOfContainers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLifetime() {
        return lifetime;
    }

    public void setLifetime(String lifetime) {
        this.lifetime = lifetime;
    }

    public List<YarnComponent> getComponents() {
        return components;
    }

    public void setComponents(List<YarnComponent> components) {
        this.components = components;
    }

    public Iterable<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("number_of_containers")
    public int getNumberOfContainers() {
        return numberOfContainers;
    }

    public void setNumberOfContainers(int numberOfContainers) {
        this.numberOfContainers = numberOfContainers;
    }

    @JsonProperty("launch_time")
    public long getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(long launchTime) {
        this.launchTime = launchTime;
    }

    @JsonProperty("number_of_running_containers")
    public int getNumberOfRunningContainers() {
        return numberOfRunningContainers;
    }

    public void setNumberOfRunningContainers(int numberOfRunningContainers) {
        this.numberOfRunningContainers = numberOfRunningContainers;
    }

    @Override
    public String toString() {
        return "ApplicationDetailResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", lifetime='" + lifetime + '\'' +
                ", components=" + components +
                ", containers=" + containers +
                ", state='" + state + '\'' +
                ", numberOfContainers=" + numberOfContainers +
                ", expectedNumberOfContainers=" + expectedNumberOfContainers +
                ", launchTime=" + launchTime +
                ", numberOfRunningContainers=" + numberOfRunningContainers +
                '}';
    }
}
