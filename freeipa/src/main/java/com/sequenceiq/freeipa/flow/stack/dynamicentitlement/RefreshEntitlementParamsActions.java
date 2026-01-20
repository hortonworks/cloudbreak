package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.dynamicentitlement.DynamicEntitlementRefreshService;

@Configuration
public class RefreshEntitlementParamsActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshEntitlementParamsActions.class);

    @Inject
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "REFRESH_FREEIPA_ENTITLEMENT_STATE")
    public Action<?, ?> refreshFreeIPAEntitlement() {
        return new AbstractRefreshEntitlementParamsAction<>(RefreshEntitlementParamsTriggerEvent.class) {

            @Override
            protected Selectable createRequest(RefreshEntitlementParamsContext context) {
                return new StackEvent(RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FINALIZED_EVENT.event(), context.getStack().getId());
            }

            @Override
            protected void doExecute(RefreshEntitlementParamsContext context, RefreshEntitlementParamsTriggerEvent payload, Map<Object, Object> variables) {
                dynamicEntitlementRefreshService.storeChangedEntitlementsAndTelemetry(context.getStack(), payload.getChangedEntitlements());
                setChainedAction(variables, payload.isChained());
                setFinalChain(variables, payload.isFinalChain());
                setOperationId(variables, payload.getOperationId());
                sendEvent(context);
            }
        };
    }

    @Bean(name = "REFRESH_ENTITLEMENT_FAILED_STATE")
    public Action<?, ?> refreshEntitlementFailedAction() {
        return new AbstractRefreshEntitlementParamsAction<>(StackFailureEvent.class) {

            @Override
            protected void doExecute(RefreshEntitlementParamsContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                String errorReason = payload.getException().getMessage();
                Stack stack = context.getStack();
                String environmentCrn = stack.getEnvironmentCrn();
                String message = String.format("Refresh entitlement based configurations failed: %s", errorReason);
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                LOGGER.info(message, payload.getException());
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPGRADE_FAILED, message);
                sendEvent(context, RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}