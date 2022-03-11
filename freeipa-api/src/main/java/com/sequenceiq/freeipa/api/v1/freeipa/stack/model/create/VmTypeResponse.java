package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmTypeResponse implements JsonEntity {

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