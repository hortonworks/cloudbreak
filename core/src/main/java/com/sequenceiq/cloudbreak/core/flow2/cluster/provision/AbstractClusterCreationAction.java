package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction.PROVISION_TYPE;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterCreationAction<P extends Payload> extends AbstractStackAction<FlowState, FlowEvent, ClusterCreationViewContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractClusterCreationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterCreationViewContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        Map<Object, Object> variables = clusterContext.getExtendedState().getVariables();
        ProvisionType provisionType = (ProvisionType) variables.getOrDefault(PROVISION_TYPE, ProvisionType.REGULAR);
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        MDCBuilder.buildMdcContext(cluster);
        return new ClusterCreationViewContext(flowParameters, stack, cluster, provisionType);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterCreationViewContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
