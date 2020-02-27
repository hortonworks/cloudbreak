package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.config.EventBusStatisticReporter;

import reactor.bus.Event;
import reactor.bus.EventBus;

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
    private StackService stackService;

    @Inject
    private EventParameterFactory eventParameterFactory;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public FlowIdentifier notify(Long stackId, String selector, Acceptable acceptable) {
        return notify(stackId, selector, acceptable, stackService::getByIdWithTransaction);
    }

    public FlowIdentifier notifyWithoutCheck(Long stackId, String selector, Acceptable acceptable) {
        return notify(stackId, selector, acceptable, stackService::getByIdWithTransaction);
    }

    public FlowIdentifier notify(Long stackId, String selector, Acceptable acceptable, Function<Long, Stack> getStackFn) {
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(eventParameterFactory.createEventParameters(stackId), acceptable);

        Stack stack = getStackFn.apply(event.getData().getResourceId());
        Optional.ofNullable(stack).map(Stack::getCluster).map(Cluster::getStatus).ifPresent(isTriggerAllowedInMaintenance(selector));
        reactorReporter.logInfoReport();
        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null) {
                reactorReporter.logErrorReport();
                throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for stack %s.", stack.getName()));
            }
            switch (accepted.getResultType()) {
                case ALREADY_EXISTING_FLOW:
                    reactorReporter.logErrorReport();
                    throw new FlowsAlreadyRunningException(String.format("Stack %s has flows under operation, request not allowed.", stack.getName()));
                case RUNNING_IN_FLOW:
                    return new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                case RUNNING_IN_FLOW_CHAIN:
                    return new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
                default:
                    throw new IllegalStateException("Unsupported accept result type: " + accepted.getClass());
            }
        } catch (InterruptedException e) {
            throw new CloudbreakApiException(e.getMessage());
        }
    }

    private Consumer<Status> isTriggerAllowedInMaintenance(String selector) {
        return status -> {
            if (Status.MAINTENANCE_MODE_ENABLED.equals(status) && !ALLOWED_FLOW_TRIGGERS_IN_MAINTENANCE.contains(selector)) {
                throw new CloudbreakApiException("Operation not allowed in maintenance mode.");
            }
        };
    }
}
