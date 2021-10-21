package com.sequenceiq.cloudbreak.core.flow2.stack;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction.PROVISION_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackFailureAction<S extends FlowState, E extends FlowEvent>
        extends AbstractStackAction<S, E, StackFailureContext, StackFailureEvent> {

    @Inject
    private StackService stackService;

    protected AbstractStackFailureAction() {
        super(StackFailureEvent.class);
    }

    @Override
    protected StackFailureContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, StackFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        flow.setFlowFailed(payload.getException());
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        ProvisionType provisionType = (ProvisionType) variables.getOrDefault(PROVISION_TYPE, ProvisionType.REGULAR);
        return new StackFailureContext(flowParameters, stack, provisionType);
    }

    @Override
    protected Object getFailurePayload(StackFailureEvent payload, Optional<StackFailureContext> flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<StackFailureEvent>> payloadConverters) {
        payloadConverters.add(new CloudPlatformResponseToStackFailureConverter());
        payloadConverters.add(new ClusterPlatformResponseToStackFailureConverter());
    }
}
