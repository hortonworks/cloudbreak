package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxInternalClusterRequest extends SdxClusterRequest {

    @Schema(description = ModelDescriptions.STACK_REQUEST)
    private StackV4Request stackV4Request;

    public StackV4Request getStackV4Request() {
        return stackV4Request;
    }

    public void setStackV4Request(StackV4Request stackV4Request) {
        this.stackV4Request = stackV4Request;
    }

    @Override
    public void addTag(String key, String value) {
        super.addTag(key, value);
        stackV4Request.addTag(key, value);
    }
}
