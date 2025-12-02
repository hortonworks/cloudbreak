package com.sequenceiq.freeipa.flow.stack;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.start.FreeIpaServiceStartService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.StackStatusCheckerJob;

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
        return new HealthCheckFailed(resourceId, event.getData().getInstanceIds(), e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<HealthCheckRequest> event) {
        HealthCheckRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            if (request.isWaitForFreeIpaAvailability()) {
                try {
                    freeIpaServiceStartService.pollFreeIpaHealth(stack);
                } catch (Exception e) {
                    LOGGER.error("FreeIpa health check failed because waiting until FreeIpa is available failed", e);
                    return new HealthCheckFailed(stackId, request.getInstanceIds(), e, ERROR);
                }
            }
            stackStatusCheckerJob.syncAStack(stack, true);
            result = new HealthCheckSuccess(stackId, request.getInstanceIds());
        } catch (Exception e) {
            LOGGER.error("FreeIpa health check failed", e);
            result = new HealthCheckFailed(stackId, request.getInstanceIds(), e, ERROR);
        }
        return result;
    }
}
