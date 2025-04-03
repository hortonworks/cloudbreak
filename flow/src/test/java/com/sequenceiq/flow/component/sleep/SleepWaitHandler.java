package com.sequenceiq.flow.component.sleep;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.component.TestStateStore;
import com.sequenceiq.flow.component.sleep.event.SleepCompletedEvent;
import com.sequenceiq.flow.component.sleep.event.SleepFailedEvent;
import com.sequenceiq.flow.component.sleep.event.SleepWaitRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

public class SleepWaitHandler extends ExceptionCatcherEventHandler<SleepWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepWaitHandler.class);

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
            PollGroup pollingStatus;
            do {
                pollingStatus = TestStateStore.get(data.getResourceId());
                if (PollGroup.CANCELLED.equals(pollingStatus)) {
                    LOGGER.info("Sleep polling was cancelled.");
                    return new SleepFailedEvent(data.getResourceId(), "Sleep polling was cancelled.");
                } else {
                    LOGGER.info("Polling status {}", pollingStatus);
                    TimeUnit.MILLISECONDS.sleep(data.getSleepDuration().toMillis());
                }
            } while (PollGroup.POLLABLE.equals(pollingStatus));
            if (data.getFailUntil().isAfter(LocalDateTime.now())) {
                return new SleepFailedEvent(data.getResourceId(), "Sleep was configured to fail.");
            } else {
                return new SleepCompletedEvent(data.getResourceId());
            }
        } catch (InterruptedException e) {
            return new SleepFailedEvent(data.getResourceId(), "Sleep was interrupted.");
        } finally {
            TestStateStore.delete(data.getResourceId());
        }
    }
}
