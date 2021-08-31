package com.sequenceiq.flow.component.sleep;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.component.sleep.event.SleepCompletedEvent;
import com.sequenceiq.flow.component.sleep.event.SleepFailedEvent;
import com.sequenceiq.flow.component.sleep.event.SleepWaitRequest;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

public class SleepWaitHandler extends ExceptionCatcherEventHandler<SleepWaitRequest> {

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SleepWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SleepWaitRequest> event) {
        SleepWaitRequest data = event.getData();
        return new SleepFailedEvent(data.getResourceId(), e.getMessage());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SleepWaitRequest> event) {
        SleepWaitRequest data = event.getData();
        try {
            TimeUnit.MILLISECONDS.sleep(data.getSleepDuration().toMillis());
            if (data.getFailUntil().isAfter(LocalDateTime.now())) {
                return new SleepFailedEvent(data.getResourceId(), "Sleep was configured to fail.");
            } else {
                return new SleepCompletedEvent(data.getResourceId());
            }
        } catch (InterruptedException e) {
            return new SleepFailedEvent(data.getResourceId(), "Sleep was interrupted.");
        }
    }
}
