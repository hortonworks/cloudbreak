package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ValidateCloudConfigRequest extends StackEvent {

    public ValidateCloudConfigRequest(Long stackId) {
        super(stackId);
    }

    public ValidateCloudConfigRequest(String selector, Long stackId) {
        super(selector, stackId);
    }

    @JsonCreator
    public ValidateCloudConfigRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
