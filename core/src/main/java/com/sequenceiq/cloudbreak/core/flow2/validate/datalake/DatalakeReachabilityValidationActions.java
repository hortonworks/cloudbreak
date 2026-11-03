package com.sequenceiq.cloudbreak.core.flow2.validate.datalake;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DatalakeReachabilityValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationRequest;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DatalakeReachabilityValidationActions {

    private static final String FAILURE_STATUS = "DATALAKE_REACHABILITY_VALIDATION_FAILURE_STATUS";

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "DATALAKE_REACHABILITY_VALIDATION_STATE")
    public Action<?, ?> datalakeReachabilityValidationAction() {
        return new AbstractDatalakeReachabilityValidationAction<>(DatalakeReachabilityValidationTriggerEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeReachabilityValidationTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(FAILURE_STATUS, payload.getFailureStatus());
                sendEvent(context, new DatalakeReachabilityValidationRequest(payload.getResourceId()));
            }
        };
    }

    @Bean(name = "DATALAKE_REACHABILITY_VALIDATION_FAILED_STATE")
    public Action<?, ?> datalakeReachabilityValidationFailureAction() {
        return new AbstractStackFailureAction<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                String statusReason = payload.getException().getMessage();
                DetailedStackStatus failureStatus = (DetailedStackStatus) variables.getOrDefault(FAILURE_STATUS, DetailedStackStatus.AVAILABLE);
                stackUpdaterService.updateStatusAndSendEventWithArgs(context.getStackId(), failureStatus,
                        ResourceEvent.DATALAKE_REACHABILITY_VALIDATION_FAILED, statusReason, statusReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_FAILURE_HANDLED_EVENT.selector(),
                        context.getStackId());
            }
        };
    }
}
