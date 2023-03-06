package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class UpdateClouderaManagerConfigResult extends CloudPlatformResult implements FlowPayload {

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    @JsonCreator
    public UpdateClouderaManagerConfigResult(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(resourceId);
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    public UpdateClouderaManagerConfigResult(String statusReason, Exception errorDetails, Long resourceId,
            StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(statusReason, errorDetails, resourceId);
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpdateClouderaManagerConfigResult.class.getSimpleName() + "[", "]")
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .toString();
    }
}
