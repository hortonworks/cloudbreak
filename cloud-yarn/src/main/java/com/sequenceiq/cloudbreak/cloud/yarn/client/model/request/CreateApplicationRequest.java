package com.sequenceiq.cloudbreak.cloud.yarn.client.model.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Configuration;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;

public class CreateApplicationRequest implements Serializable {

    private String name;

    private int lifetime;

    private String queue;

    private Configuration configuration;

    private List<YarnComponent> components = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public List<YarnComponent> getComponents() {
        return components;
    }

    public void setComponents(List<YarnComponent> components) {
        this.components = components;
    }

    @Override
    public String toString() {
        return "CreateApplicationRequest{"
                + "name=" + name
                + ", lifetime=" + lifetime
                + ", components='" + components
                + '}';
    }

}
