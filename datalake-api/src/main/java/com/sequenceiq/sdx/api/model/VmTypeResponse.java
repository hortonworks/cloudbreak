package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmTypeResponse {

    @Schema(description = ModelDescriptions.VM_TYPE_NAME)
    private String value;

    @Schema(description = ModelDescriptions.VM_TYPE_METADATA)
    private VmTypeMetaJson vmTypeMetaJson;

    public VmTypeResponse() {
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
        return "VmTypeResponse{" + "value='" + value + '\'' + ", vmTypeMetaJson=" + vmTypeMetaJson + '}';
    }
}
