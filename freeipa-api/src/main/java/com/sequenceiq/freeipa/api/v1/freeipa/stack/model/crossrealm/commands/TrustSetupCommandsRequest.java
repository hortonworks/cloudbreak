package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustSetupCommandsV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustSetupCommandsRequest extends TrustSetupCommandsBase {
    @Override
    public String toString() {
        return "TrustSetupCommandsRequest{} " + super.toString();
    }
}
