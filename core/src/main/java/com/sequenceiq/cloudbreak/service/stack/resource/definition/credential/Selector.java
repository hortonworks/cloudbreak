package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Selector {

    @JsonProperty("name")
    private String name;

    @JsonProperty("parent")
    private String parent;

    @JsonProperty("values")
    private List<Value> values = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Collection<Value> getValues() {
        return values;
    }

    public void setValues(List<Value> values) {
        this.values = values;
    }
}
