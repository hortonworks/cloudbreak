package com.sequenceiq.flow.reactor.api.event;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.model.ResultType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class EventSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSender.class);

    private static final long TIMEOUT = 10L;

    private final EventBus reactor;

    private final ErrorHandlerAwareReactorEventFactory eventFactory;

    public EventSender(EventBus reactor, ErrorHandlerAwareReactorEventFactory eventFactory) {
        this.reactor = reactor;
        this.eventFactory = eventFactory;
    }

    public Optional<FlowIdentifier> sendEvent(BaseNamedFlowEvent event, Event.Headers headers) {
        return doSend(event, headers, event.getResourceName());
    }

    public Optional<FlowIdentifier> sendEvent(BaseFlowEvent event, Event.Headers headers) {
        return doSend(event, headers, null);
    }

    private Optional<FlowIdentifier> doSend(BaseFlowEvent event, Event.Headers headers, String resourceName) {
        Event<BaseFlowEvent> eventWithErrHandler = eventFactory.createEventWithErrHandler(new HashMap<>(headers.asMap()), event);
        reactor.notify(event.selector(), eventWithErrHandler);
        if (eventWithErrHandler.getData().accepted() != null) {
            try {
                FlowAcceptResult accepted = (FlowAcceptResult) eventWithErrHandler.getData().accepted().await(TIMEOUT, TimeUnit.SECONDS);
                if (accepted == null || ResultType.ALREADY_EXISTING_FLOW.equals(accepted.getResultType())) {
                    throw new IllegalStateException(String.format("Resource with name: '%s' and crn: '%s' has flow under operation, request is not allowed.",
                            resourceName, event.getResourceCrn()));
                }
                switch (accepted.getResultType()) {
                    case RUNNING_IN_FLOW:
                        return Optional.of(new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId()));
                    case RUNNING_IN_FLOW_CHAIN:
                        return Optional.of(new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId()));
                    default:
                        throw new IllegalStateException("Illegal resultType: " + accepted.getResultType());
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e.getMessage());
            }
        } else {
            LOGGER.debug("Received event with no associated flow event. Nothing to do.");
            return Optional.empty();
        }
    }
}
