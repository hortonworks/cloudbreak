package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResetJvmParamsV4Response {

    @Schema(description = "Flow identifier of the triggered reset JVM params operation")
    private FlowIdentifier flowIdentifier;

    @Schema(description = "Human-readable message about the reset JVM params operation")
    private String message;

    @Schema(description = "Diff of JVM parameter changes computed before the operation was triggered")
    private ResetJvmParamsDiffV4Response resetJvmParamsDiff;

    public ResetJvmParamsV4Response() {
    }

    public ResetJvmParamsV4Response(FlowIdentifier flowIdentifier, String message) {
        this.flowIdentifier = flowIdentifier;
        this.message = message;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResetJvmParamsDiffV4Response getResetJvmParamsDiff() {
        return resetJvmParamsDiff;
    }

    public void setResetJvmParamsDiff(ResetJvmParamsDiffV4Response resetJvmParamsDiff) {
        this.resetJvmParamsDiff = resetJvmParamsDiff;
    }

    @Override
    public String toString() {
        return "ResetJvmParamsV4Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", message='" + message + '\'' +
                ", resetJvmParamsDiff=" + resetJvmParamsDiff +
                '}';
    }
}
