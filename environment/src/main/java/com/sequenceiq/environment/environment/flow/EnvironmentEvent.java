package com.sequenceiq.environment.environment.flow;

import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public interface EnvironmentEvent extends IdempotentEvent<BaseFlowEvent>, ResourceCrnPayload {
    EnvironmentDto getEnvironmentDto();
}
