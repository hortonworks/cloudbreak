package com.sequenceiq.cloudbreak.orchestrator.yarn.model.request;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;

public class CreateApplicationRequest implements JsonEntity {

    private String name;

    private int lifetime;

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
