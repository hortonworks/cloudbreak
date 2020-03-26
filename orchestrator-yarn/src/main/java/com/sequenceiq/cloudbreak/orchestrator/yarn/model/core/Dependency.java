package com.sequenceiq.cloudbreak.orchestrator.yarn.model.core;

import com.sequenceiq.common.model.JsonEntity;

public class Dependency implements JsonEntity {

    private String item;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
