package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VmTypeResponse {

    private String vmType;

    private VmTypeMetaResponse metaData;

    private Boolean extended;

    public VmTypeResponse() {
    }

    public VmTypeResponse(String vmType, VmTypeMetaResponse meta, Boolean extended) {
        this.vmType = vmType;
        metaData = meta;
        this.extended = extended;
    }

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
    }

    public void setMetaData(VmTypeMetaResponse metaData) {
        this.metaData = metaData;
    }

    public Boolean getExtended() {
        return extended;
    }

    public void setExtended(Boolean extended) {
        this.extended = extended;
    }

    public VmTypeMetaResponse getMetaData() {
        return metaData;
    }

    public String getMetaDataValue(String key) {
        return metaData.getProperties().get(key);
    }

    public boolean isMetaSet() {
        return metaData != null;
    }
}
