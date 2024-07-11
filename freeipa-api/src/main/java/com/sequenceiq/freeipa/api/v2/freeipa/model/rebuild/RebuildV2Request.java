package com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RebuildV2Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RebuildV2Request extends RebuildV2Base {
    @Override
    public String toString() {
        return "RebuildV2Request{} " + super.toString();
    }
}
