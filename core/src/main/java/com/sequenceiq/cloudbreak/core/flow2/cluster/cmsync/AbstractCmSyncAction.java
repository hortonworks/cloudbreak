package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractCmSyncAction<P extends Payload> extends AbstractStackAction<CmSyncState, CmSyncEvent, CmSyncContext, P> {
    @Inject
    private StackService stackService;

    protected AbstractCmSyncAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CmSyncContext createFlowContext(FlowParameters flowParameters, StateContext<CmSyncState, CmSyncEvent> stateContext, P payload) {
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack.getClusterView());
        return new CmSyncContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CmSyncContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
