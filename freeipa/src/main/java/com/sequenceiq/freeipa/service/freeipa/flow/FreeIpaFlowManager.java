package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.config.EventBusStatisticReporter;
import com.sequenceiq.flow.service.FlowNameFormatService;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;

@Component
public class FreeIpaFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFlowManager.class);

    private static final long WAIT_FOR_ACCEPT = 10L;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private FlowNameFormatService flowNameFormatService;

    @Inject
    private EventBusStatisticReporter reactorReporter;

    public FlowIdentifier notify(String selector, Acceptable acceptable) {
        Map<String, Object> headerWithUserCrn = getHeaderWithUserCrn(null);
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(headerWithUserCrn, acceptable);
        notify(selector, event);
        return checkFlowOperationForResource(event);
    }

    public void notify(Selectable selectable) {
        Event<Selectable> event = eventFactory.createEvent(selectable);
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selectable.selector(), event);
        reactorReporter.logInfoReport();
        reactor.notify(selectable.selector(), event);
    }

    public FlowIdentifier notify(BaseFlowEvent selectable, Event.Headers headers) {
        Event<BaseFlowEvent> event = eventFactory.createEventWithErrHandler(new HashMap<>(headers.asMap()), selectable);
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selectable.selector(), event);
        reactorReporter.logInfoReport();
        reactor.notify(selectable.selector(), event);
        return checkFlowOperationForResource(event);
    }

    private void notify(String selector, Event<Acceptable> event) {
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selector, event);
        reactorReporter.logInfoReport();
        reactor.notify(selector, event);
        checkFlowOperationForResource(event);
    }

    private FlowIdentifier checkFlowOperationForResource(Event<? extends Acceptable> event) {
        try {
            Promise<AcceptResult> acceptPromise = event.getData().accepted();
            FlowAcceptResult accepted = (FlowAcceptResult) Benchmark.checkedMeasure(() -> acceptPromise.await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS), LOGGER,
                    "Accepting flow event took {}ms");
            if (accepted == null) {
                reactorReporter.logErrorReport();
                throw new RuntimeException("FlowAcceptResult was null. Maybe flow is under operation, request not allowed.");
            }
            switch (accepted.getResultType()) {
                case RUNNING_IN_FLOW:
                    return new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                case RUNNING_IN_FLOW_CHAIN:
                    return new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
                case ALREADY_EXISTING_FLOW:
                    throw new FlowsAlreadyRunningException(String.format("Request not allowed, freeipa cluster already has a running operation. " +
                                    "Running operation(s): [%s]",
                            flowNameFormatService.formatFlows(accepted.getAlreadyRunningFlows())));
                default:
                    throw new IllegalStateException("Illegal resultType: " + accepted.getResultType());
            }
        } catch (InterruptedException e) {
            reactorReporter.logErrorReport();
            throw new RuntimeException(e.getMessage());
        }
    }

    private Map<String, Object> getHeaderWithUserCrn(Map<String, Object> headers) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Map<String, Object> decoratedHeader;
        decoratedHeader = headers != null ? new HashMap<>(headers) : new HashMap<>();
        if (StringUtils.isNotBlank(userCrn)) {
            decoratedHeader.put(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
        }
        return decoratedHeader;
    }
}
