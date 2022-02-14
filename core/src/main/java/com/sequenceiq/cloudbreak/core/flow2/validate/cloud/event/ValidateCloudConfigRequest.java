package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ValidateCloudConfigRequest extends StackEvent {

    public ValidateCloudConfigRequest(Long stackId) {
        super(stackId);
    }

    public ValidateCloudConfigRequest(String selector, Long stackId) {
        super(selector, stackId);
    }

    public ValidateCloudConfigRequest(String selector, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
