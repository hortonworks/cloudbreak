package com.sequenceiq.freeipa.flow.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.start.FreeIpaServiceStartService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.StackStatusCheckerJob;

import reactor.bus.Event;

@Component
public class HealthCheckHandler extends ExceptionCatcherEventHandler<HealthCheckRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckHandler.class);

    @Inject
    private FreeIpaServiceStartService freeIpaServiceStartService;

    @Inject
    private StackService stackService;

    @Inject
    private StackStatusCheckerJob stackStatusCheckerJob;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(HealthCheckRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<HealthCheckRequest> event) {
        return new HealthCheckFailed(resourceId, event.getData().getInstanceIds(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<HealthCheckRequest> event) {
        HealthCheckRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            if (request.getWaitForFreeIpaAvailability()) {
                try {
                    freeIpaServiceStartService.pollFreeIpaHealth(stack);
                } catch (Exception e) {
                    LOGGER.error("FreeIpa health check failed because waiting until FreeIpa is available failed", e);
                    return new HealthCheckFailed(stackId, request.getInstanceIds(), e);
                }
            }
            stackStatusCheckerJob.syncAStack(stack, true);
            result = new HealthCheckSuccess(stackId, request.getInstanceIds());
        } catch (Exception e) {
            LOGGER.error("FreeIpa health check failed", e);
            result = new HealthCheckFailed(stackId, request.getInstanceIds(), e);
        }
        return result;
    }
}
