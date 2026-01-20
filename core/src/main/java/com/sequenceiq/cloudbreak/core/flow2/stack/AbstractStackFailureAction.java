package com.sequenceiq.cloudbreak.core.flow2.stack;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction.PROVISION_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.PayloadConverter;

public abstract class AbstractStackFailureAction<S extends FlowState, E extends FlowEvent>
        extends AbstractStackAction<S, E, StackFailureContext, StackFailureEvent> {

    public static final String CONCLUSION_CHECKER_TYPE_KEY = "ConclusionCheckerType";

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractStackFailureAction() {
        super(StackFailureEvent.class);
    }

    @Override
    protected StackFailureContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, StackFailureEvent payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        ProvisionType provisionType = (ProvisionType) variables.getOrDefault(PROVISION_TYPE, ProvisionType.REGULAR);
        return new StackFailureContext(flowParameters, stack, stack.getId(), provisionType, getConclusionCheckerType(variables));
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

    protected ConclusionCheckerType getConclusionCheckerType(Map<Object, Object> variables) {
        Object conclusionCheckerType = MapUtils.emptyIfNull(variables).getOrDefault(CONCLUSION_CHECKER_TYPE_KEY, ConclusionCheckerType.DEFAULT);
        return conclusionCheckerType instanceof ConclusionCheckerType ? (ConclusionCheckerType) conclusionCheckerType : ConclusionCheckerType.DEFAULT;
    }
}
