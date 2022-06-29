package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.AbstractUpgradeRdsEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractUpgradeRdsAction<P extends AbstractUpgradeRdsEvent> extends AbstractStackAction<FlowState, FlowEvent, UpgradeRdsContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractUpgradeRdsAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected UpgradeRdsContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(stack.getClusterView());
        return new UpgradeRdsContext(flowParameters, stack, payload.getVersion());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpgradeRdsContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    public StackService getStackService() {
        return stackService;
    }
}
