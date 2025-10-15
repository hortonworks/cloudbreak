package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.config.EventBusStatisticReporter;
import com.sequenceiq.flow.service.FlowNameFormatService;

@Service
public class ReactorNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorNotifier.class);

    private static final long WAIT_FOR_ACCEPT = 10L;

    private static final List<String> ALLOWED_FLOW_TRIGGERS_IN_MAINTENANCE = List.of(
            FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT,
            FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT,
            FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT,
            FlowChainTriggers.TERMINATION_TRIGGER_EVENT,
            FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT,
            StackTerminationEvent.TERMINATION_EVENT.event()
    );

    @Inject
    private EventBus reactor;

    @Inject
    private EventBusStatisticReporter reactorReporter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EventParameterFactory eventParameterFactory;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private FlowNameFormatService flowNameFormatService;

    @Inject
    private NodeValidator nodeValidator;

    public FlowIdentifier notify(Long stackId, String selector, Acceptable acceptable) {
        return notify(stackId, selector, acceptable, stackDtoService::getStackViewById);
    }

    public FlowIdentifier notifyWithoutCheck(Long stackId, String selector, Acceptable acceptable) {
        return notify(stackId, selector, acceptable, stackDtoService::getStackViewById);
    }

    public FlowIdentifier notify(BaseFlowEvent selectable, Event.Headers headers) {
        nodeValidator.checkForRecentHeartbeat();
        Event<BaseFlowEvent> event = eventFactory.createEventWithErrHandler(new HashMap<>(headers.asMap()), selectable);
        LOGGER.debug("Notify reactor for selector [{}] with event [{}]", selectable.selector(), event);
        reactor.notify(selectable.selector(), event);
        return checkFlowStatus(event, selectable.getResourceCrn());
    }

    public FlowIdentifier notify(Long stackId, String selector, Acceptable acceptable, Function<Long, StackView> getStackFn) {
        nodeValidator.checkForRecentHeartbeat();
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(eventParameterFactory.createEventParameters(stackId), acceptable);
        StackView stack = getStackFn.apply(event.getData().getResourceId());
        Optional.ofNullable(stack).map(StackView::getStatus).ifPresent(isTriggerAllowedInMaintenance(selector));
        reactorReporter.logInfoReport();
        reactor.notify(selector, event);
        return checkFlowStatus(event, stack.getName());
    }

    private FlowIdentifier checkFlowStatus(Event<? extends Acceptable> event, String identifier) {
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null) {
                LOGGER.error("Event not accepted: {}", event);
                reactorReporter.logErrorReport();
                throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for stack %s.", identifier));
            }
            return switch (accepted.getResultType()) {
                case ALREADY_EXISTING_FLOW -> {
                    reactorReporter.logErrorReport();
                    throw new FlowsAlreadyRunningException(String.format("Request not allowed, cluster '%s' already has a running operation. " +
                                    "Running operation(s): [%s]",
                            identifier,
                            flowNameFormatService.formatFlows(accepted.getAlreadyRunningFlows())));
                }
                case RUNNING_IN_FLOW -> new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                case RUNNING_IN_FLOW_CHAIN -> new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
            };
        } catch (InterruptedException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Consumer<Status> isTriggerAllowedInMaintenance(String selector) {
        return status -> {
            if (Status.MAINTENANCE_MODE_ENABLED.equals(status) && !ALLOWED_FLOW_TRIGGERS_IN_MAINTENANCE.contains(selector)) {
                throw new CloudbreakServiceException("Operation not allowed in maintenance mode.");
            }
        };
    }
}