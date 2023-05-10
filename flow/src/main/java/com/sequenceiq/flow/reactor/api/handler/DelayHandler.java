package com.sequenceiq.flow.reactor.api.handler;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.event.DelayEvent;
import com.sequenceiq.flow.reactor.api.event.DelayFailedEvent;

@Component
public class DelayHandler extends ExceptionCatcherEventHandler<DelayEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayHandler.class);

    @Inject
    private Optional<DelayedExecutorService> delayedExecutorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DelayEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DelayEvent> event) {
        LOGGER.error("Delay failed unexpectedly", e);
        return event.getData().sendSuccessInCaseOfFailure() ? event.getData().successEvent() : new DelayFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DelayEvent> event) {
        DelayEvent delayEvent = event.getEvent().getData();
        if (delayedExecutorService.isPresent()) {
            try {
                LOGGER.debug("Delay execution");
                return delayedExecutorService.get().runWithDelay(delayEvent::successEvent, delayEvent.delayInSec(), TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error("Delay failed", e);
                return delayEvent.sendSuccessInCaseOfFailure() ? delayEvent.successEvent() : new DelayFailedEvent(delayEvent.resourceId(), e);
            }
        } else {
            LOGGER.warn("No 'DelayedExecutorService' available, skipping delay");
            return delayEvent.successEvent();
        }
    }
}
