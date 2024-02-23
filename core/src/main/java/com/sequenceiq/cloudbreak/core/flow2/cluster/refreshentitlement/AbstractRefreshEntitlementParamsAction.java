package com.sequenceiq.cloudbreak.core.flow2.cluster.refreshentitlement;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractRefreshEntitlementParamsAction<P extends Payload>
        extends AbstractStackAction<RefreshEntitlementParamsState, RefreshEntitlementParamsEvent, RefreshEntitlementParamsContext, P> {

    @Inject
    private StackDtoService stackService;

    protected AbstractRefreshEntitlementParamsAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected RefreshEntitlementParamsContext createFlowContext(FlowParameters flowParameters, StateContext<RefreshEntitlementParamsState,
            RefreshEntitlementParamsEvent> stateContext, P payload) {
        StackDto stack = stackService.getById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack.getCluster());
        return new RefreshEntitlementParamsContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RefreshEntitlementParamsContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
