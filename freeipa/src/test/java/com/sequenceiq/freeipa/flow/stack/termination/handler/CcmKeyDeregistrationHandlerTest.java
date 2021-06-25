package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.termination.event.ccm.CcmKeyDeregistrationRequest;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class CcmKeyDeregistrationHandlerTest {

    private static final String ACTOR_CRN = "actorCrn";

    private static final String ACCOUNT = "account";

    private static final String KEY_ID = "key";

    private static final String MINA_SSHD_SERVICE_ID = "mina";

    private static final String AGENT_CRN = "agentCrn";

    @Mock
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Mock
    private EventBus eventBus;

    @Mock
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @InjectMocks
    private CcmKeyDeregistrationHandler underTest;

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCMV2", "CCMV2_JUMPGATE" }, mode = EnumSource.Mode.EXCLUDE)
    void testWhenTunnelIsNotCcmV2ThenNoCcmV2AgentDeregistrationIsCalled(Tunnel tunnel) {
        CcmKeyDeregistrationRequest request =
                new CcmKeyDeregistrationRequest(null, null, ACTOR_CRN, ACCOUNT, KEY_ID, tunnel, null, AGENT_CRN);
        Event<CcmKeyDeregistrationRequest> event = new Event<>(request);
        underTest.accept(event);
        verifyNoMoreInteractions(ccmV2AgentTerminationListener);
    }

    @Test
    void testWhenTunnelIsCcmThenCcmDeregistrationIsCalled() {
        CcmKeyDeregistrationRequest request =
                new CcmKeyDeregistrationRequest(null, null, ACTOR_CRN, ACCOUNT, KEY_ID, Tunnel.CCM, MINA_SSHD_SERVICE_ID, AGENT_CRN);
        Event<CcmKeyDeregistrationRequest> event = new Event<>(request);
        underTest.accept(event);
        verify(ccmResourceTerminationListener).deregisterCcmSshTunnelingKey(ACTOR_CRN, ACCOUNT, KEY_ID, MINA_SSHD_SERVICE_ID);
        verifyNoMoreInteractions(ccmV2AgentTerminationListener);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCMV2", "CCMV2_JUMPGATE" }, mode = EnumSource.Mode.INCLUDE)
    void testWhenTunnelIsCcmV2ThenCcmV2AgentDeregistrationIsCalled(Tunnel tunnel) {
        CcmKeyDeregistrationRequest request =
                new CcmKeyDeregistrationRequest(null, null, ACTOR_CRN, ACCOUNT, KEY_ID, tunnel, null, AGENT_CRN);
        Event<CcmKeyDeregistrationRequest> event = new Event<>(request);
        underTest.accept(event);
        verify(ccmV2AgentTerminationListener).deregisterInvertingProxyAgent(AGENT_CRN);
        verifyNoMoreInteractions(ccmResourceTerminationListener);
    }
}
