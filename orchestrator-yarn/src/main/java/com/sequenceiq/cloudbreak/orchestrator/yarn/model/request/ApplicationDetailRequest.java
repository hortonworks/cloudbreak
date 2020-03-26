package com.sequenceiq.cloudbreak.orchestrator.yarn.model.request;

import com.sequenceiq.common.model.JsonEntity;

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
