package com.sequenceiq.flow.reactor.api.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public class BaseNamedFlowEvent extends BaseFlowEvent {

    private final String resourceName;

    public BaseNamedFlowEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceCrn);
        this.resourceName = resourceName;
    }

    @JsonCreator
    public BaseNamedFlowEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn) {

        super(selector, resourceId, resourceCrn, accepted);
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
