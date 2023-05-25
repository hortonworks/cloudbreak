package com.sequenceiq.flow.rotation.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.rotation.status.event.RotationStatusChangeEvent;

public class SecretRotationStatusChangeFlowContext extends CommonContext {

    private Long resourceId;

    private String resourceCrn;

    private boolean start;

    @JsonCreator
    public SecretRotationStatusChangeFlowContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("start") boolean start) {
        super(flowParameters);
        this.resourceId = resourceId;
        this.resourceCrn = resourceCrn;
        this.start = start;
    }

    public static SecretRotationStatusChangeFlowContext fromPayload(FlowParameters flowParameters,
            RotationStatusChangeEvent rotationStatusChangeEvent) {
        return new SecretRotationStatusChangeFlowContext(flowParameters, rotationStatusChangeEvent.getResourceId(),
                rotationStatusChangeEvent.getResourceCrn(), rotationStatusChangeEvent.isStart());
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public boolean isStart() {
        return start;
    }
}
