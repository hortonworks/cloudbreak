package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VmTypeResponse implements Serializable {
    private String value;

    private VmTypeMetaJson vmTypeMetaJson;

    public VmTypeResponse() {

    }

    public VmTypeResponse(String value) {
        this.value = value;
    }

    public VmTypeResponse(String value, VmTypeMetaJson vmTypeMetaJson) {
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

    @Override
    public String toString() {
        return "VmTypeResponse{" +
                "value='" + value + '\'' +
                ", vmTypeMetaJson=" + vmTypeMetaJson +
                '}';
    }
}
