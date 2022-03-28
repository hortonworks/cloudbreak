package com.sequenceiq.cloudbreak.orchestrator.yarn.model.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact implements JsonEntity {

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
}
