package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("InstanceGroupNetworkV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceGroupNetworkRequest extends InstanceGroupNetworkBase {

    @Override
    public String toString() {
        return super.toString() + ", InstanceGroupNetworkRequest{}";
    }
}
