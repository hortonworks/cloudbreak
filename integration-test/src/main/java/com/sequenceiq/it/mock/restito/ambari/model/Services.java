package com.sequenceiq.it.mock.restito.ambari.model;

import com.google.gson.annotations.SerializedName;

public class Services {

    @SerializedName("RootServiceComponents")
    private RootServiceComponents rootServiceComponents;

    public Services(RootServiceComponents rootServiceComponents) {
        this.rootServiceComponents = rootServiceComponents;
    }

    public RootServiceComponents getRootServiceComponents() {
        return rootServiceComponents;
    }

    public void setRootServiceComponents(RootServiceComponents rootServiceComponents) {
        this.rootServiceComponents = rootServiceComponents;
    }
}
