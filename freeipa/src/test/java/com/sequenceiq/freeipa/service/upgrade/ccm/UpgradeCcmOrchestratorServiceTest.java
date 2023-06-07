package com.sequenceiq.freeipa.service.upgrade.ccm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    @Mock
    private OrchestratorParamsProvider orchestratorParamsProvider;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private Stack stack;

    @InjectMocks
    private UpgradeCcmOrchestratorService underTest;

    @Mock
    private OrchestratorStateParams stateParams;

    @Mock
    private OrchestratorRunParams runParams;

    @Test
    void testApplyUpgradeState() throws CloudbreakOrchestratorException {
        when(orchestratorParamsProvider.createStateParams(eq(STACK_ID), any())).thenReturn(stateParams);
        underTest.applyUpgradeState(STACK_ID);
        verifyState("upgradeccm");
    }

    @Test
    void testReconfigureNginx() throws CloudbreakOrchestratorException {
        when(orchestratorParamsProvider.createStateParams(eq(STACK_ID), any())).thenReturn(stateParams);
        underTest.reconfigureNginx(STACK_ID);
        verifyState("nginx");
    }

    @Test
    void testFinalize() throws CloudbreakOrchestratorException {
        when(orchestratorParamsProvider.createStateParams(eq(STACK_ID), any())).thenReturn(stateParams);
        underTest.finalizeConfiguration(STACK_ID);
        verifyState("upgradeccm/finalize");
    }

    @Test
    void testDisableMina() throws CloudbreakOrchestratorException {
        when(orchestratorParamsProvider.createStateParams(eq(STACK_ID), any())).thenReturn(stateParams);
        underTest.disableMina(STACK_ID);
        verifyState("upgradeccm/disable-ccmv1");
    }

    @Test
    void testCcmV2ConnectivityCheck() {
        when(orchestratorParamsProvider.createRunParams(eq(STACK_ID), eq("ccmv2-connectivity-check.sh someCidr"), anyString()))
                .thenReturn(runParams);
        underTest.checkCcmV2Connectivity(STACK_ID, "someCidr");
        verify(orchestratorParamsProvider).createRunParams(eq(STACK_ID), eq("ccmv2-connectivity-check.sh someCidr"), anyString());
        verify(hostOrchestrator).runShellCommandOnNodes(runParams);
    }

    private void verifyState(String state) throws CloudbreakOrchestratorException {
        verify(orchestratorParamsProvider).createStateParams(STACK_ID, state);
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

}
