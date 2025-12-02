package com.sequenceiq.freeipa.flow.stack.stop.handler;

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
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StopFreeIpaServicesEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaServicesStopService;

@Component
public class StopFreeIpaServicesHandler extends ExceptionCatcherEventHandler<StopFreeIpaServicesEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StopFreeIpaServicesHandler.class);

    @Inject
    private FreeIpaServicesStopService freeIpaServicesStopService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopFreeIpaServicesEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopFreeIpaServicesEvent> event) {
        return new StackFailureEvent(StackStopEvent.STOP_FAILURE_EVENT.event(), resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StopFreeIpaServicesEvent> event) {
        try {
            freeIpaServicesStopService.stopServices(event.getData().getResourceId());
        } catch (Exception e) {
            LOGGER.error("FreeIPA service stop failed. Continue with stopping instances", e);
        }
        return new StackEvent(StackStopEvent.STACK_STOP_INSTANCES_EVENT.event(), event.getData().getResourceId());
    }
}
