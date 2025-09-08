package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCreationFailedRequest extends StackEvent {

    private final ConclusionCheckerType conclusionCheckerType;

    @JsonCreator
    public ClusterCreationFailedRequest(@JsonProperty("resourceId") Long stackId,
            @JsonProperty("conclusionCheckerType") ConclusionCheckerType conclusionCheckerType) {
        super(stackId);
        this.conclusionCheckerType = conclusionCheckerType;
    }

    public ConclusionCheckerType getConclusionCheckerType() {
        return conclusionCheckerType;
    }
}
