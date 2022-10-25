package com.sequenceiq.freeipa.flow.stack.modify.proxy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigSaltStateApplyRequest;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.service.proxy.ModifyProxyConfigOrchestratorService;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigSaltStateApplyHandlerTest {

    private static final long STACK_ID = 1L;

    private static final HandlerEvent<ModifyProxyConfigSaltStateApplyRequest> EVENT =
            new HandlerEvent<>(Event.wrap(new ModifyProxyConfigSaltStateApplyRequest(STACK_ID)));

    @InjectMocks
    private ModifyProxyConfigSaltStateApplyHandler underTest;

    @Mock
    private ModifyProxyConfigOrchestratorService modifyProxyConfigOrchestratorService;

    @Test
    void defaultFailureEvent() {
        Exception cause = new Exception("cause");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, cause, mock(Event.class));

        assertThat(result)
                .isInstanceOf(StackFailureEvent.class)
                .extracting(StackFailureEvent.class::cast)
                .returns(STACK_ID, StackFailureEvent::getResourceId)
                .returns(cause, StackFailureEvent::getException);
    }

    @Test
    void doAccept() throws CloudbreakOrchestratorException {
        Selectable result = underTest.doAccept(EVENT);

        verify(modifyProxyConfigOrchestratorService).applyModifyProxyState(STACK_ID);
        assertThat(result)
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_SUCCESS_EVENT.selector(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);
    }

    @Test
    void doAcceptFailure() throws CloudbreakOrchestratorException {
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("cause");
        doThrow(cause).when(modifyProxyConfigOrchestratorService).applyModifyProxyState(STACK_ID);

        assertThatThrownBy(() -> underTest.doAccept(EVENT))
                .isInstanceOf(CloudbreakRuntimeException.class)
                .hasMessage("Failed to apply modify proxy orchestrator state: " + cause.getMessage())
                .hasCause(cause);
    }

}
