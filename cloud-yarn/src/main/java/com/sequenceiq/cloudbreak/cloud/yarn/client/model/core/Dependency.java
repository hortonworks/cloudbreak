package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

public class Dependency implements JsonEntity {

    private String item;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
