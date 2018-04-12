package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Definition {

    @JsonProperty("values")
    private List<Value> defaultValues = new ArrayList<>();

    @JsonProperty("selectors")
    private List<Selector> selectors = new ArrayList<>();

    public Iterable<Value> getDefaultValues() {
        return defaultValues;
    }

    public void setValues(List<Value> values) {
        defaultValues = values;
    }

    public List<Selector> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<Selector> selectors) {
        this.selectors = selectors;
    }
}
