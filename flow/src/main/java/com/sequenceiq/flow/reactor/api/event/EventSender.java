package com.sequenceiq.flow.reactor.api.event;

import static com.sequenceiq.flow.core.FlowConstants.FLOW_CHAIN_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_ID;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.config.EventBusStatisticReporter;
import com.sequenceiq.flow.service.FlowNameFormatService;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;

@Component
public class EventSender {

    private static final Logger LOGGER = getLogger(EventSender.class);

    private static final long TIMEOUT = 10L;

    private final EventBus reactor;

    private final ErrorHandlerAwareReactorEventFactory eventFactory;

    private final FlowNameFormatService flowNameFormatService;

    private final EventBusStatisticReporter reactorReporter;

    public EventSender(EventBus reactor, ErrorHandlerAwareReactorEventFactory eventFactory, FlowNameFormatService flowNameFormatService,
            EventBusStatisticReporter reactorReporter) {
        this.reactor = reactor;
        this.eventFactory = eventFactory;
        this.flowNameFormatService = flowNameFormatService;
        this.reactorReporter = reactorReporter;
    }

    public FlowIdentifier sendEvent(BaseNamedFlowEvent event, Event.Headers headers) {
        return doSend(event, headers, event.getResourceName());
    }

    public FlowIdentifier sendEvent(BaseFlowEvent event, Event.Headers headers) {
        return doSend(event, headers, null);
    }

    private FlowIdentifier doSend(BaseFlowEvent event, Event.Headers headers, String resourceName) {
        Event<BaseFlowEvent> eventWithErrHandler = eventFactory.createEventWithErrHandler(new HashMap<>(headers.asMap()), event);
        reactor.notify(event.selector(), eventWithErrHandler);
        Promise<AcceptResult> accepted = eventWithErrHandler.getData().accepted();
        String resourceCrn = event.getResourceCrn();
        if (accepted != null) {
            try {
                FlowAcceptResult acceptResult = (FlowAcceptResult) accepted.await(TIMEOUT, TimeUnit.SECONDS);
                return createFlowIdentifier(acceptResult, resourceCrn);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
        if (headers.contains(FLOW_ID)) {
            return new FlowIdentifier(FlowType.FLOW, headers.get(FLOW_ID));
        } else if (headers.contains(FLOW_CHAIN_ID)) {
            return new FlowIdentifier(FlowType.FLOW_CHAIN, headers.get(FLOW_CHAIN_ID));
        }
        LOGGER.error("Accepted is null, header does not contains flow or flow chain id, event: {}, header: {}", event, headers);
        reactorReporter.logErrorReport();
        throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for stack %s.", resourceCrn));
    }

    private FlowIdentifier createFlowIdentifier(FlowAcceptResult accepted, String resourceCrn) {
        switch (accepted.getResultType()) {
            case ALREADY_EXISTING_FLOW:
                reactorReporter.logErrorReport();
                throw new FlowsAlreadyRunningException(String.format("Request not allowed, cluster '%s' already has a running operation. " +
                                "Running operation(s): [%s]",
                        resourceCrn,
                        flowNameFormatService.formatFlows(accepted.getAlreadyRunningFlows())));
            case RUNNING_IN_FLOW:
                return new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
            case RUNNING_IN_FLOW_CHAIN:
                return new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
            default:
                throw new IllegalStateException("Unsupported accept result type: " + accepted.getClass());
        }
    }
}
