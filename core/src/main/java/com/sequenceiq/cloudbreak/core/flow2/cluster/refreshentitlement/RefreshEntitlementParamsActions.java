package com.sequenceiq.cloudbreak.core.flow2.cluster.refreshentitlement;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RefreshEntitlementParamsTriggerEvent;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;

@Configuration
public class RefreshEntitlementParamsActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshEntitlementParamsActions.class);

    private static final String EVENT_TYPE = "DYNAMIC_ENTITLEMENT";

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Bean(name = "REFRESH_CB_ENTITLEMENT_STATE")
    public Action<?, ?> refreshCbEntitlement() {
        return new AbstractRefreshEntitlementParamsAction<>(RefreshEntitlementParamsTriggerEvent.class) {

            @Override
            protected Selectable createRequest(RefreshEntitlementParamsContext context) {
                return new StackEvent(RefreshEntitlementParamsEvent.CONFIGURE_MANAGEMENT_SERVICES_EVENT.event(), context.getStack().getId());
            }

            @Override
            protected void doExecute(RefreshEntitlementParamsContext context, RefreshEntitlementParamsTriggerEvent payload, Map<Object, Object> variables) {
                dynamicEntitlementRefreshService.storeChangedEntitlementsInTelemetry(payload.getResourceId(), payload.getChangedEntitlements());
                sendEvent(context);
            }
        };
    }

    @Bean(name = "RE_CONFIGURE_MANAGEMENT_SERVICES_STATE")
    public Action<?, ?> configureManagementServicesAction() {
        return new AbstractRefreshEntitlementParamsAction<>(StackEvent.class) {
            @Override
            protected void doExecute(RefreshEntitlementParamsContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RefreshEntitlementParamsContext context) {
                return new ConfigureClusterManagerManagementServicesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "RE_CONFIGURE_MANAGEMENT_SERVICES_FINISHED_STATE")
    public Action<?, ?> configureManagementServicesFinishedAction() {
        return new AbstractRefreshEntitlementParamsAction<>(ConfigureClusterManagerManagementServicesSuccess.class) {
            @Override
            protected void doExecute(RefreshEntitlementParamsContext context, ConfigureClusterManagerManagementServicesSuccess payload,
                    Map<Object, Object> variables) {
                flowMessageService.fireEventAndLog(context.getStack().getId(), EVENT_TYPE, ResourceEvent.STACK_DYNAMIC_ENTITLEMENT_FINISHED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RefreshEntitlementParamsContext context) {
                return new StackEvent(RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "REFRESH_ENTITLEMENT_FAILED_STATE")
    public Action<?, ?> refreshEntitlementFailedAction() {
        return new AbstractStackFailureAction<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Error during refreshing dynamic entitlements.", payload.getException());
                flowMessageService.fireEventAndLog(context.getStackId(), EVENT_TYPE, ResourceEvent.STACK_DYNAMIC_ENTITLEMENT_FAILED,
                        payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}
