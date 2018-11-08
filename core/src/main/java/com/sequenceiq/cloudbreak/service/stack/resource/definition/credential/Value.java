package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Value {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("optional")
    private Boolean optional;

    @JsonProperty("sensitive")
    private Boolean sensitive = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public Boolean getSensitive() {
        return sensitive;
    }

    public void setSensitive(Boolean sensitive) {
        this.sensitive = sensitive;
    }
}
