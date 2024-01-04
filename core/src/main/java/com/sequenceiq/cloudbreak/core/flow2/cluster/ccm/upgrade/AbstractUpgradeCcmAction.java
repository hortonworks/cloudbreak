package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmBaseEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractUpgradeCcmAction<P extends UpgradeCcmBaseEvent> extends AbstractStackAction<FlowState, FlowEvent, UpgradeCcmContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractUpgradeCcmAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected UpgradeCcmContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(cluster);
        return new UpgradeCcmContext(flowParameters, stack, cluster, payload.getOldTunnel(), payload.getRevertTime());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpgradeCcmContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
