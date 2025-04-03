package com.sequenceiq.datalake.flow.enableselinux.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class DatalakeEnableSeLinuxHandlerEvent extends BaseNamedFlowEvent {

    @JsonCreator
    public DatalakeEnableSeLinuxHandlerEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn) {
        super(EventSelectorUtil.selector(DatalakeEnableSeLinuxHandlerEvent.class), resourceId, resourceName, resourceCrn);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DatalakeEnableSeLinuxHandlerEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("resourceId=" + getResourceId())
                .add("resourceName=" + getResourceName())
                .add("resourceCrn=" + getResourceCrn())
                .toString();
    }
}
