package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import java.io.Serializable;

public class Artifact implements Serializable {

    private String id;

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Artifact {"
                + "id=" + id
                + ", type=" + type
                + '}';
    }
}
