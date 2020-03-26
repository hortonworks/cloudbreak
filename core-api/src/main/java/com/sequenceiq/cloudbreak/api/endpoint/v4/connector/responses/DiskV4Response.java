package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskV4Response implements JsonEntity {

    private String type;

    private String name;

    private String displayName;

    public DiskV4Response() {
    }

    public DiskV4Response(String name, String type, String displayName) {
        this.type = type;
        this.name = name;
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
