package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService.CCM_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService.CCM_UPGRADE_FINISHED_BUT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
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
        underTest.ccmUpgradeFinished(1L, 1L, Boolean.TRUE);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.AVAILABLE), eq(CCM_UPGRADE_FINISHED));
    }

    @Test
    void ccmUpgradeFinishedBut() {
        underTest.ccmUpgradeFinished(1L, 1L, Boolean.FALSE);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.AVAILABLE), eq(CCM_UPGRADE_FINISHED_BUT));
    }
}