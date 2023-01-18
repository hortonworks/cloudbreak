package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaUpscaleV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpscaleRequest extends ScaleRequestBase {

    @Override
    public String toString() {
        return "UpscaleRequest{} " + super.toString();
    }
}
