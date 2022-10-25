package com.sequenceiq.freeipa.service.upgrade.ccm;

import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorStateParamsProvider;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    @Mock
    private OrchestratorStateParamsProvider orchestratorStateParamsProvider;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private Stack stack;

    @InjectMocks
    private UpgradeCcmOrchestratorService underTest;

    @Mock
    private OrchestratorStateParams stateParams;

    @BeforeEach
    void setUp() {
        when(orchestratorStateParamsProvider.createStateParams(eq(STACK_ID), any())).thenReturn(stateParams);
    }

    @Test
    void testApplyUpgradeState() throws CloudbreakOrchestratorException {
        underTest.applyUpgradeState(STACK_ID);
        verifyState("upgradeccm");
    }

    @Test
    void testReconfigureNginx() throws CloudbreakOrchestratorException {
        underTest.reconfigureNginx(STACK_ID);
        verifyState("nginx");
    }

    @Test
    void testFinalize() throws CloudbreakOrchestratorException {
        underTest.finalizeConfiguration(STACK_ID);
        verifyState("upgradeccm/finalize");
    }

    @Test
    void testDisableMina() throws CloudbreakOrchestratorException {
        underTest.disableMina(STACK_ID);
        verifyState("upgradeccm/disable-ccmv1");
    }

    private void verifyState(String state) throws CloudbreakOrchestratorException {
        verify(orchestratorStateParamsProvider).createStateParams(STACK_ID, state);
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

}
