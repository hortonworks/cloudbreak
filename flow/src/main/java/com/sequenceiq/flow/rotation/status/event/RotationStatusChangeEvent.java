package com.sequenceiq.flow.rotation.status.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class RotationStatusChangeEvent extends BaseFlowEvent {

    private final boolean start;

    @JsonCreator
    protected RotationStatusChangeEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("start") boolean start,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.start = start;
    }

    protected RotationStatusChangeEvent(String selector, Long resourceId, String resourceCrn, boolean start) {
        super(selector, resourceId, resourceCrn);
        this.start = start;
    }

    public boolean isStart() {
        return start;
    }
}
