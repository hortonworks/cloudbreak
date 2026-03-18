package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractUpdateTrustedRealmAction<P extends Payload>
        extends AbstractStackAction<UpdateTrustedRealmState, UpdateTrustedRealmEvent, UpdateTrustedRealmContext, P> {

    protected static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    protected static final String REALM = "REALM";

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractUpdateTrustedRealmAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected UpdateTrustedRealmContext createFlowContext(FlowParameters flowParameters,
            StateContext<UpdateTrustedRealmState, UpdateTrustedRealmEvent> stateContext, P payload) {
        StackView stackView = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stackView);
        String environmentCrn = (String) stateContext.getExtendedState().getVariables().get(ENVIRONMENT_CRN);
        String realm = (String) stateContext.getExtendedState().getVariables().get(REALM);
        return new UpdateTrustedRealmContext(flowParameters, stackView, environmentCrn, realm);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpdateTrustedRealmContext> flowContext, Exception ex) {
        return new UpdateTrustedRealmFailureEvent(payload.getResourceId(), ex);
    }
}


