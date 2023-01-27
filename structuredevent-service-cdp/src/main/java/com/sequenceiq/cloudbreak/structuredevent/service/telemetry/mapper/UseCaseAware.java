package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import com.sequenceiq.flow.core.FlowState;

public interface UseCaseAware<E> {

    E getUseCaseForFlowState(Enum<? extends FlowState> flowState);
}
