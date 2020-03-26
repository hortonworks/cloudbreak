package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VmTypeV4Response implements JsonEntity {
    private String value;

    private VmTypeMetaJson vmTypeMetaJson;

    public VmTypeV4Response() {

    }

    public VmTypeV4Response(String value) {
        this.value = value;
    }

    public VmTypeV4Response(String value, VmTypeMetaJson vmTypeMetaJson) {
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
