package com.sequenceiq.environment.environment.flow.loadbalancer.event;

import static com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors.FAILED_LOAD_BALANCER_UPDATE_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class LoadBalancerUpdateFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    public LoadBalancerUpdateFailedEvent(EnvironmentDto environmentDto, Exception exception, EnvironmentStatus environmentStatus) {
        super(FAILED_LOAD_BALANCER_UPDATE_EVENT.name(), environmentDto.getResourceId(),
            environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.environmentDto = environmentDto;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_LOAD_BALANCER_UPDATE_EVENT.event();
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
