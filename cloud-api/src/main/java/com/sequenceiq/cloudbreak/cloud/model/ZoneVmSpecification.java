package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoneVmSpecification {
    @JsonProperty("zone")
    private String zone;

    @JsonProperty("vmTypes")
    private List<String> vmTypes;

    @JsonProperty("defaultVmType")
    private String defaultVmType;

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public List<String> getVmTypes() {
        return vmTypes;
    }

    public void setVmTypes(List<String> vmTypes) {
        this.vmTypes = vmTypes;
    }

    public String getDefaultVmType() {
        return defaultVmType;
    }

    public void setDefaultVmType(String defaultVmType) {
        this.defaultVmType = defaultVmType;
    }
}
