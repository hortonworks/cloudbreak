package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaDownscaleV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownscaleResponse extends ScaleResponseBase {

    @Override
    public String toString() {
        return "DownscaleResponse{} " + super.toString();
    }
}