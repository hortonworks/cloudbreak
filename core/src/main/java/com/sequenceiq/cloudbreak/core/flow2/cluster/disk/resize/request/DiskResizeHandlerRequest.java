package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class DiskResizeHandlerRequest extends BaseFlowEvent implements Selectable {

    private final String instanceGroup;

    @JsonCreator
    public DiskResizeHandlerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup) {
        super(selector, stackId, null);
        this.instanceGroup = instanceGroup;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public String toString() {
        return new StringJoiner(", ", DiskResizeHandlerRequest.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }
}
