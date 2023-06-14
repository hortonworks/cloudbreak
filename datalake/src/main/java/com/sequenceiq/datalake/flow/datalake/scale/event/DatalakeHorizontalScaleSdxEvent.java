package com.sequenceiq.datalake.flow.datalake.scale.event;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;

public class DatalakeHorizontalScaleSdxEvent extends SdxEvent {

    private final String resourceCrn;

    private final DatalakeHorizontalScaleRequest scaleRequest;

    private final Exception exception;

    private final String flowId;

    private final BigDecimal commandId;

    @JsonCreator
    public DatalakeHorizontalScaleSdxEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String resourceName,
            @JsonProperty("userId") String userId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("scaleRequest") DatalakeHorizontalScaleRequest scaleRequest,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("flowId") String flowId,
            @JsonProperty("commandId") BigDecimal commandId) {
        super(selector, sdxId, resourceName, userId);
        this.resourceCrn = resourceCrn;
        this.scaleRequest = scaleRequest;
        this.exception = exception;
        this.flowId = flowId;
        this.commandId = commandId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public DatalakeHorizontalScaleRequest getScaleRequest() {
        return scaleRequest;
    }

    public Exception getException() {
        return exception;
    }

    public String getFlowId() {
        return flowId;
    }

    public BigDecimal getCommandId() {
        return commandId;
    }
}
