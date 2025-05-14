package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorStateParamsProvider;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private OrchestratorStateParamsProvider orchestratorStateParamsProvider;

    @Mock
    private OrchestratorStateParams stateParams;

    @InjectMocks
    private UpgradeCcmOrchestratorService underTest;

    @BeforeEach
    void setUp() {
        when(orchestratorStateParamsProvider.createStateParams(eq(STACK_ID), anyString())).thenReturn(stateParams);
    }

    @Test
    void testReconfigureNginx() throws CloudbreakOrchestratorException {
        underTest.reconfigureNginx(STACK_ID);
        verifyOrchestratorState("nginx/upgradeccm");
    }

    @Test
    void testDisableMina() throws CloudbreakOrchestratorException {
        underTest.disableMina(STACK_ID);
        verifyOrchestratorState("upgradeccm/disable-ccmv1");
    }

    @Test
    void testDisableInvertingProxyAgent() throws CloudbreakOrchestratorException {
        underTest.disableInvertingProxyAgent(STACK_ID);
        verifyOrchestratorState("upgradeccm/disable-ccmv2");
    }

    @Test
    void testFinalizeCcmOperation() throws CloudbreakOrchestratorException {
        underTest.finalizeCcmOperation(STACK_ID);
        verifyOrchestratorState("nginx/finalize");
    }

    private void verifyOrchestratorState(String state) throws CloudbreakOrchestratorFailedException {
        verify(orchestratorStateParamsProvider).createStateParams(STACK_ID, state);
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

}
