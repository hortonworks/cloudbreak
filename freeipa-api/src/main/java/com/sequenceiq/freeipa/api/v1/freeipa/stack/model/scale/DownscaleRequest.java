package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeIpaDownscaleV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownscaleRequest extends ScaleRequestBase {

    @Override
    public String toString() {
        return "DownscaleRequest{} " + super.toString();
    }
}
