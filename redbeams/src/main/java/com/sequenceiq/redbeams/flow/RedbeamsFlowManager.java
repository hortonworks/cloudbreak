package com.sequenceiq.redbeams.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.FlowNameFormatService;

@Component
public class RedbeamsFlowManager {

    private static final long WAIT_FOR_ACCEPT = 10L;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private FlowNameFormatService flowNameFormatService;

    public FlowIdentifier notify(String selector, Acceptable acceptable) {
        Map<String, Object> headerWithUserCrn = getHeaderWithUserCrn(null);
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(headerWithUserCrn, acceptable);
        return notify(selector, event);
    }

    public FlowIdentifier triggerSecretRotation(Long resourceId, String resourceCrn, List<SecretType> secretTypes, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        String selector = EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
        return notify(selector, new SecretRotationFlowChainTriggerEvent(selector, resourceId, resourceCrn, secretTypes, executionType, additionalProperties));
    }

    private FlowIdentifier notify(String selector, Event<Acceptable> event) {
        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null) {
                throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for database %s.",
                        event.getData().getResourceId()));
            } else {
                switch (accepted.getResultType()) {
                    case ALREADY_EXISTING_FLOW:
                        throw new FlowsAlreadyRunningException(String.format(
                                "Request not allowed, external database already has a running operation. Running operation(s): [%s]",
                                flowNameFormatService.formatFlows(accepted.getAlreadyRunningFlows())));
                    case RUNNING_IN_FLOW:
                        return new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                    case RUNNING_IN_FLOW_CHAIN:
                        return new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
                    default:
                        throw new IllegalStateException("Unsupported accept result type: " + accepted.getClass());
                }
            }

        } catch (InterruptedException e) {
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
