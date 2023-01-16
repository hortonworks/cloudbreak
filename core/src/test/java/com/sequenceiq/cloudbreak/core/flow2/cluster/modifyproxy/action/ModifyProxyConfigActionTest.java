package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigStatusService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFailureResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.ActionTest;

abstract class ModifyProxyConfigActionTest<P extends StackEvent> extends ActionTest {

    static final long STACK_ID = 1L;

    static final String PREVIOUS_PROXY_CONFIG_CRN = "proxy-crn";

    @Mock
    ModifyProxyConfigStatusService modifyProxyConfigStatusService;

    @Mock
    StackDtoService stackDtoService;

    ModifyProxyConfigContext context;

    @Mock
    StateContext<ModifyProxyConfigState, ModifyProxyConfigEvent> stateContext;

    @Mock
    ExtendedState extendedState;

    @Mock
    StackView stackView;

    abstract ModifyProxyConfigAction<P> getAction();

    abstract P getEvent();

    @BeforeEach
    void setUp() {
        context = new ModifyProxyConfigContext(flowParameters, stackView, PREVIOUS_PROXY_CONFIG_CRN);
        lenient().when(stateContext.getExtendedState()).thenReturn(extendedState);
        lenient().when(getEvent().getResourceId()).thenReturn(STACK_ID);
        lenient().when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        lenient().when(stackView.getId()).thenReturn(STACK_ID);
    }

    @Test
    void createFlowContext() {
        String previousProxyConfigCrn = "prev-proxy-crn";
        when(extendedState.get(ModifyProxyConfigAction.PREVIOUS_PROXY_CONFIG, String.class)).thenReturn(previousProxyConfigCrn);

        ModifyProxyConfigContext result = getAction().createFlowContext(flowParameters, stateContext, getEvent());

        assertThat(result)
                .returns(flowParameters, ModifyProxyConfigContext::getFlowParameters)
                .returns(stackView, ModifyProxyConfigContext::getStack)
                .returns(previousProxyConfigCrn, ModifyProxyConfigContext::getPreviousProxyConfigCrn);
    }

    @Test
    void getFailurePayload() {
        Exception cause = new Exception("cause");

        Object result = getAction().getFailurePayload(getEvent(), Optional.empty(), cause);

        assertThat(result)
                .isInstanceOf(ModifyProxyConfigFailureResponse.class)
                .extracting(ModifyProxyConfigFailureResponse.class::cast)
                .returns(STACK_ID, ModifyProxyConfigFailureResponse::getResourceId)
                .returns(cause, ModifyProxyConfigFailureResponse::getException);
    }

}
