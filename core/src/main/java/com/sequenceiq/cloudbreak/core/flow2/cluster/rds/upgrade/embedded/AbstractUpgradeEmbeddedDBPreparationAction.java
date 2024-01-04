package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.AbstractUpgradeEmbeddedDBPreparationEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractUpgradeEmbeddedDBPreparationAction<P extends AbstractUpgradeEmbeddedDBPreparationEvent>
        extends AbstractStackAction<FlowState, FlowEvent, UpgradeEmbeddedDBPreparationContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractUpgradeEmbeddedDBPreparationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected UpgradeEmbeddedDBPreparationContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext,
                                                                    P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(cluster);
        return new UpgradeEmbeddedDBPreparationContext(flowParameters, stack, cluster, payload.getVersion());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpgradeEmbeddedDBPreparationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
