package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VmTypeJson implements JsonEntity {
    private String value;

    private VmTypeMetaJson vmTypeMetaJson;

    public VmTypeJson() {

    }

    public VmTypeJson(String value) {
        this.value = value;
    }

    public VmTypeJson(String value, VmTypeMetaJson vmTypeMetaJson) {
        this.value = value;
        this.vmTypeMetaJson = vmTypeMetaJson;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public VmTypeMetaJson getVmTypeMetaJson() {
        return vmTypeMetaJson;
    }

    public void setVmTypeMetaJson(VmTypeMetaJson vmTypeMetaJson) {
        this.vmTypeMetaJson = vmTypeMetaJson;
    }
}
