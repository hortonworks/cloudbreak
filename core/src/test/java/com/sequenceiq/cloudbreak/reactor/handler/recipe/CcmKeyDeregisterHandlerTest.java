package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import static com.sequenceiq.common.api.type.Tunnel.CCMV2_JUMPGATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.Tunnel;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class CcmKeyDeregisterHandlerTest {

    private static final String ACTOR_CRN = "actorCrn";

    private static final String ACCOUNT = "account";

    private static final String KEY_ID = "key";

    private static final String MINA_SSHD_SERVICE_ID = "mina";

    private static final String AGENT_CRN = "agentCrn";

    private static final long STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Mock
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    @InjectMocks
    private CcmKeyDeregisterHandler underTest;

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCMV2", "CCMV2_JUMPGATE" }, mode = EnumSource.Mode.EXCLUDE)
    void testWhenTunnelIsNotCcmV2ThenNoCcmV2AgentDeregistrationIsCalled(Tunnel tunnel) {
        setupBasicStack();

        CcmKeyDeregisterRequest request =
                new CcmKeyDeregisterRequest(STACK_ID, ACTOR_CRN, ACCOUNT, KEY_ID, tunnel);
        Event<CcmKeyDeregisterRequest> event = new Event<>(request);

        underTest.accept(event);

        verifyNoMoreInteractions(ccmV2AgentTerminationListener);
        checkSuccess();
    }

    @Test
    void testWhenTunnelIsCcmThenCcmDeregistrationIsCalled() {
        Stack stack = setupBasicStack();
        stack.setMinaSshdServiceId(MINA_SSHD_SERVICE_ID);

        CcmKeyDeregisterRequest request =
                new CcmKeyDeregisterRequest(STACK_ID, ACTOR_CRN, ACCOUNT, KEY_ID, Tunnel.CCM);
        Event<CcmKeyDeregisterRequest> event = new Event<>(request);

        underTest.accept(event);

        verify(ccmResourceTerminationListener).deregisterCcmSshTunnelingKey(ACTOR_CRN, ACCOUNT, KEY_ID, MINA_SSHD_SERVICE_ID);
        verifyNoMoreInteractions(ccmV2AgentTerminationListener);
        checkSuccess();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCMV2", "CCMV2_JUMPGATE" }, mode = EnumSource.Mode.INCLUDE)
    void testWhenTunnelIsCcmV2ThenCcmV2AgentDeregistrationIsCalled(Tunnel tunnel) {
        Stack stack = setupBasicStack();
        stack.setCcmV2AgentCrn(AGENT_CRN);

        CcmKeyDeregisterRequest request =
                new CcmKeyDeregisterRequest(STACK_ID, ACTOR_CRN, ACCOUNT, KEY_ID, tunnel);
        Event<CcmKeyDeregisterRequest> event = new Event<>(request);

        underTest.accept(event);

        verify(ccmV2AgentTerminationListener).deregisterInvertingProxyAgent(AGENT_CRN);
        verifyNoMoreInteractions(ccmResourceTerminationListener);
        checkSuccess();
    }

    @Test
    void testWhenTunnelIsCcmV2JumpgateAndNoAgentRegisteredThenCcmV2AgentDeregistrationIsNeverCalled() {
        Stack stack = setupBasicStack();
        stack.setCcmV2AgentCrn(EMPTY);

        CcmKeyDeregisterRequest request =
                new CcmKeyDeregisterRequest(STACK_ID, ACTOR_CRN, ACCOUNT, KEY_ID, CCMV2_JUMPGATE);
        Event<CcmKeyDeregisterRequest> event = new Event<>(request);

        underTest.accept(event);

        verify(ccmV2AgentTerminationListener, never()).deregisterInvertingProxyAgent(AGENT_CRN);
        verifyNoMoreInteractions(ccmResourceTerminationListener);
        checkSuccess();
    }

    @Test
    void testWhenErrorHappensDuringDeregistrationStillSuccess() {
        Stack stack = setupBasicStack();
        stack.setCcmV2AgentCrn(AGENT_CRN);

        CcmKeyDeregisterRequest request =
                new CcmKeyDeregisterRequest(STACK_ID, ACTOR_CRN, ACCOUNT, KEY_ID, CCMV2_JUMPGATE);
        Event<CcmKeyDeregisterRequest> event = new Event<>(request);

        doAnswer(a -> {
            throw new Exception("failed");
        }).when(ccmV2AgentTerminationListener).deregisterInvertingProxyAgent(anyString());
        underTest.accept(event);

        verifyNoMoreInteractions(ccmResourceTerminationListener);
        verifyNoMoreInteractions(ccmV2AgentTerminationListener);
        checkSuccess();
    }

    @Test
    void testWhenErrorHappensDuringStackRetrieval() {
        CcmKeyDeregisterRequest request =
                new CcmKeyDeregisterRequest(STACK_ID, ACTOR_CRN, ACCOUNT, KEY_ID, CCMV2_JUMPGATE);
        Event<CcmKeyDeregisterRequest> event = new Event<>(request);

        doAnswer(a -> {
            throw new Exception("failed");
        }).when(stackService).getByIdWithListsInTransaction(anyLong());
        underTest.accept(event);

        verifyNoMoreInteractions(ccmResourceTerminationListener);
        verifyNoMoreInteractions(ccmV2AgentTerminationListener);
        checkFailure();
    }

    private Stack setupBasicStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn("resourceCrn");
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        return stack;
    }

    private void checkSuccess() {
        verify(eventBus).notify((Object) any(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData()).isInstanceOf(CcmKeyDeregisterSuccess.class);
    }

    private void checkFailure() {
        verify(eventBus).notify((Object) any(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getData()).isInstanceOf(StackFailureEvent.class);
    }
}
