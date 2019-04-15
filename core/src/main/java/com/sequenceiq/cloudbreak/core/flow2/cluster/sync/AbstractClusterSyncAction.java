package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractClusterSyncAction<P extends Payload> extends AbstractStackAction<ClusterSyncState, ClusterSyncEvent, ClusterSyncContext, P> {
    @Inject
    private StackService stackService;

    protected AbstractClusterSyncAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterSyncContext createFlowContext(String flowId, StateContext<ClusterSyncState, ClusterSyncEvent> stateContext, P payload) {
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getStackId());
        MDCBuilder.buildMdcContext(stack.getId().toString(), stack.getName(), "CLUSTER");
        return new ClusterSyncContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterSyncContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }
}
