package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class RotateSaltPasswordHandler extends ExceptionCatcherEventHandler<RotateSaltPasswordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordHandler.class);

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RotateSaltPasswordRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RotateSaltPasswordRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new RotateSaltPasswordFailureResponse(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RotateSaltPasswordRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            StackDto stack = stackDtoService.getById(stackId);
            clusterBootstrapper.rotateSaltPassword(stack);
            return new RotateSaltPasswordSuccessResponse(stackId);
        } catch (Exception e) {
            LOGGER.warn("Failed to rotate salt password", e);
            return new RotateSaltPasswordFailureResponse(stackId, e);
        }
    }
}
