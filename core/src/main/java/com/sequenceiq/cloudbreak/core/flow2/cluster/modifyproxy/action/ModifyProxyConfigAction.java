package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigStatusService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class ModifyProxyConfigAction<P extends StackEvent>
        extends AbstractStackAction<ModifyProxyConfigState, ModifyProxyConfigEvent, ModifyProxyConfigContext, P> {

    static final String PREVIOUS_PROXY_CONFIG = "previousProxyConfig";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ModifyProxyConfigStatusService modifyProxyConfigStatusService;

    protected ModifyProxyConfigAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ModifyProxyConfigContext createFlowContext(FlowParameters flowParameters,
            StateContext<ModifyProxyConfigState, ModifyProxyConfigEvent> stateContext, P payload) {
        StackView stackView = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stackView);
        String previousProxyConfigCrn = stateContext.getExtendedState().get(PREVIOUS_PROXY_CONFIG, String.class);
        return new ModifyProxyConfigContext(flowParameters, stackView, previousProxyConfigCrn);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ModifyProxyConfigContext> flowContext, Exception ex) {
        return new ModifyProxyConfigFailureResponse(payload.getResourceId(), ex);
    }

    protected ModifyProxyConfigStatusService modifyProxyConfigStatusService() {
        return modifyProxyConfigStatusService;
    }
}
