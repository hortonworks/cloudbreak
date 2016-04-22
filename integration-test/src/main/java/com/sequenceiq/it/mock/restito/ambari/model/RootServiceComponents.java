package com.sequenceiq.it.mock.restito.ambari.model;

import com.google.gson.annotations.SerializedName;

public class RootServiceComponents {

    @SerializedName("component_version")
    private String componentVersion;

    public RootServiceComponents(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }
}
