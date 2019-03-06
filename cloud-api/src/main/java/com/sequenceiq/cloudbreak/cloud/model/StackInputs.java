package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackInputs {

    private Map<String, Object> customInputs;

    private Map<String, Object> fixInputs;

    private Map<String, Object> datalakeInputs;

    public StackInputs(
            @JsonProperty("customInputs") Map<String, Object> customInputs,
            @JsonProperty("fixInputs") Map<String, Object> fixInputs,
            @JsonProperty("datalakeInputs") Map<String, Object> datalakeInputs) {
        this.customInputs = customInputs;
        this.fixInputs = fixInputs;
        this.datalakeInputs = datalakeInputs;
    }

    public Map<String, Object> getCustomInputs() {
        return customInputs;
    }

    public void setCustomInputs(Map<String, Object> customInputs) {
        this.customInputs = customInputs;
    }

    public Map<String, Object> getFixInputs() {
        return fixInputs;
    }

    public void setFixInputs(Map<String, Object> fixInputs) {
        this.fixInputs = fixInputs;
    }

    public Map<String, Object> getDatalakeInputs() {
        return datalakeInputs;
    }

    public void setDatalakeInputs(Map<String, Object> datalakeInputs) {
        this.datalakeInputs = datalakeInputs;
    }

    @Override
    public String toString() {
        return "StackInputs{"
                + "customInputs=" + customInputs
                + "fixInputs=" + fixInputs
                + "datalakeInputs=" + datalakeInputs
                + '}';
    }
}
