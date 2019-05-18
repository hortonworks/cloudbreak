package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskV1Response implements Serializable {

    private String type;

    private String name;

    private String displayName;

    public DiskV1Response() {
    }

    public DiskV1Response(String name, String type, String displayName) {
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
