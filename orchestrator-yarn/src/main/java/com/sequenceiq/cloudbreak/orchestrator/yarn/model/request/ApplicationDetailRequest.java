package com.sequenceiq.cloudbreak.orchestrator.yarn.model.request;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

public class ApplicationDetailRequest implements JsonEntity {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ApplicationDetailRequest{"
                + "name=" + name
                + '}';
    }
}
