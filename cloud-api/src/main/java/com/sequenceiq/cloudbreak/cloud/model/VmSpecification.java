package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VmSpecification {
    @JsonProperty("value")
    private String value;
    @JsonProperty("extended")
    private Boolean extended = true;
    @JsonProperty("meta")
    private MetaSpecification metaSpecification;

    public VmSpecification() {
    }

    public Boolean getExtended() {
        return extended;
    }

    public void setExtended(Boolean extended) {
        this.extended = extended;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public MetaSpecification getMetaSpecification() {
        return metaSpecification;
    }

    public void setMetaSpecification(MetaSpecification metaSpecification) {
        this.metaSpecification = metaSpecification;
    }
}
