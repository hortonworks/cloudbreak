package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.AbstractUpgradeCcmEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractUpgradeCcmAction<P extends AbstractUpgradeCcmEvent> extends AbstractStackAction<FlowState, FlowEvent, UpgradeCcmContext, P> {

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
        return new UpgradeCcmContext(flowParameters, stack, cluster, payload.getOldTunnel());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpgradeCcmContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
