package com.sequenceiq.flow.reactor.api.event;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.model.ResultType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class EventSender {

    private static final long TIMEOUT = 10L;

    private final EventBus reactor;

    private final ErrorHandlerAwareReactorEventFactory eventFactory;

    public EventSender(EventBus reactor, ErrorHandlerAwareReactorEventFactory eventFactory) {
        this.reactor = reactor;
        this.eventFactory = eventFactory;
    }

    public void sendEvent(BaseNamedFlowEvent event, Event.Headers headers) {
        doSend(event, headers, event.getResourceName());
    }

    public void sendEvent(BaseFlowEvent event, Event.Headers headers) {
        doSend(event, headers, null);
    }

    private void doSend(BaseFlowEvent event, Event.Headers headers, String resourceName) {
        Event<BaseFlowEvent> eventWithErrHandler = eventFactory.createEventWithErrHandler(new HashMap<>(headers.asMap()), event);
        reactor.notify(event.selector(), eventWithErrHandler);
        if (eventWithErrHandler.getData().accepted() != null) {
            try {
                FlowAcceptResult accepted = (FlowAcceptResult) eventWithErrHandler.getData().accepted().await(TIMEOUT, TimeUnit.SECONDS);
                if (accepted == null || ResultType.ALREADY_EXISTING_FLOW.equals(accepted.getResultType())) {
                    throw new IllegalStateException(String.format("Resource with name: '%s' and crn: '%s' has flow under operation, request is not allowed.",
                            resourceName, event.getResourceCrn()));
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
    }
}
