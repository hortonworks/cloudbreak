package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService.CCM_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService.CCM_UPGRADE_FINISHED_BUT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.HealthCheckService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.UpgradeCcmOrchestratorService;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterServiceRunner clusterServiceRunner;

    @Mock
    private UpgradeCcmOrchestratorService upgradeCcmOrchestratorService;

    @Mock
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Mock
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private HealthCheckService healthCheckService;

    @InjectMocks
    private UpgradeCcmService underTest;

    @Test
    void ccmUpgradeFinished() {
        underTest.ccmUpgradeFinished(STACK_ID, STACK_ID, Boolean.TRUE);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.AVAILABLE), eq(CCM_UPGRADE_FINISHED));
    }

    @Test
    void ccmUpgradeFinishedBut() {
        underTest.ccmUpgradeFinished(STACK_ID, STACK_ID, Boolean.FALSE);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.AVAILABLE), eq(CCM_UPGRADE_FINISHED_BUT));
    }

    @Test
    @DisplayName("Test healthCheck: if there are no unhealthy hosts, then CloudbreakServiceException should not be thrown")
    void testHealthCheckIfThereAreUnhealthyHosts() {
        when(healthCheckService.getUnhealthyHosts(STACK_ID)).thenReturn(Set.of("host-worker-1"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.healthCheck(STACK_ID));

        assertEquals("One or more instances are not available. Need to roll back CCM upgrade to previous version.",
                exception.getMessage());
    }

    @Test
    @DisplayName("Test healthCheck: if getting unhealthy hosts fails with a RuntimeException, then CloudbreakServiceException should be thrown")
    void testHealthCheckIfHealthCheckServiceThrowsException() {
        when(healthCheckService.getUnhealthyHosts(STACK_ID)).thenThrow(new RuntimeException("Something happened during health check"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.healthCheck(STACK_ID));

        assertEquals("Cannot get host statuses, CM is likely not accessible. Need to roll back CCM upgrade to previous version.",
                exception.getMessage());
    }

    @Test
    @DisplayName("Test healthCheck: if no problem is found with the hosts for the given stack, then no exception should be thrown")
    void testHealthCheckIfNoProblemFound() {
        when(healthCheckService.getUnhealthyHosts(STACK_ID)).thenReturn(Set.of());

        assertDoesNotThrow(() -> underTest.healthCheck(STACK_ID));
    }
}