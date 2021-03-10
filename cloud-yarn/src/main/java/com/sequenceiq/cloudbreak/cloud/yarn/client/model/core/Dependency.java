package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import java.io.Serializable;

public class Dependency implements Serializable {

    private String item;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return item;
    }
}
