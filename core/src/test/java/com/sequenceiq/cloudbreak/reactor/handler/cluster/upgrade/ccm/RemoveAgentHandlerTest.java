package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RemoveAgentHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final Long CLUSTER_ID = 34L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private HandlerEvent<UpgradeCcmRemoveAgentRequest> event;

    @InjectMocks
    private RemoveAgentHandler underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADECCMREMOVEAGENTREQUEST");
    }

    @Test
    void doAccept() throws CloudbreakOrchestratorException {
        UpgradeCcmRemoveAgentRequest request = new UpgradeCcmRemoveAgentRequest(STACK_ID, CLUSTER_ID, Tunnel.CCM, null);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        InOrder inOrder = inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService).updateTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        inOrder.verify(upgradeCcmService).removeAgent(STACK_ID, Tunnel.CCM);
        assertThat(((UpgradeCcmRemoveAgentResult) result).getAgentDeletionSucceed()).isEqualTo(Boolean.TRUE);
        assertThat(result.selector()).isEqualTo("UPGRADECCMREMOVEAGENTRESULT");
    }

    @Test
    void orchestrationException() throws CloudbreakOrchestratorException {
        UpgradeCcmRemoveAgentRequest request = new UpgradeCcmRemoveAgentRequest(STACK_ID, CLUSTER_ID, Tunnel.CCM, null);
        when(event.getData()).thenReturn(request);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(upgradeCcmService).removeAgent(any(), any());

        Selectable result = underTest.doAccept(event);
        verify(upgradeCcmService).removeAgent(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmService).removeAgentFailed(STACK_ID);
        assertThat(result.selector()).isEqualTo("UPGRADECCMREMOVEAGENTRESULT");
        assertThat(result).isInstanceOf(UpgradeCcmRemoveAgentResult.class);
        UpgradeCcmRemoveAgentResult eventInCaseOfException = (UpgradeCcmRemoveAgentResult) result;
        assertThat(eventInCaseOfException.getOldTunnel()).isEqualTo(Tunnel.CCM);
        assertThat(eventInCaseOfException.getResourceId()).isEqualTo(STACK_ID);
        assertThat(eventInCaseOfException.getAgentDeletionSucceed()).isEqualTo(Boolean.FALSE);
    }

}
