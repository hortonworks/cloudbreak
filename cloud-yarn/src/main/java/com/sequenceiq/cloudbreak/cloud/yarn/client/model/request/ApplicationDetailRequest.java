package com.sequenceiq.cloudbreak.cloud.yarn.client.model.request;

import java.io.Serializable;

public class ApplicationDetailRequest implements Serializable {

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
