package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractClusterUpgradeAction<P extends Payload>
    extends AbstractStackAction<ClusterUpgradeState, ClusterUpgradeEvent, ClusterViewContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractClusterUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterViewContext createFlowContext(String flowId, StateContext<ClusterUpgradeState, ClusterUpgradeEvent> stateContext, P payload) {
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getStackId());
        MDCBuilder.buildMdcContext(stack.getId().toString(), stack.getName(), "CLUSTER");
        return new ClusterViewContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterViewContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }
}
