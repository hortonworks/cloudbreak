package com.sequenceiq.datalake.flow.verticalscale.addvolumes.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeAddVolumesEvent extends SdxEvent {

    private final StackAddVolumesRequest stackAddVolumesRequest;

    private final String sdxName;

    @JsonCreator
    public DatalakeAddVolumesEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("userId") String userId,
            @JsonProperty("stackAddVolumesRequest") StackAddVolumesRequest stackAddVolumesRequest,
            @JsonProperty("sdxName") String sdxName) {
        super(selector, resourceId, userId);
        this.stackAddVolumesRequest = stackAddVolumesRequest;
        this.sdxName = sdxName;
    }

    public StackAddVolumesRequest getStackAddVolumesRequest() {
        return stackAddVolumesRequest;
    }

    @Override
    public String getSdxName() {
        return sdxName;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DatalakeAddVolumesEvent.class.getSimpleName() + "[", "]")
                .add("stackAddVolumesRequest=" + stackAddVolumesRequest)
                .add("sdxName=" + sdxName)
                .toString();
    }
}
