package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAILED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerUpgradeManagementService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterManagerUpgradeHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterManagerUpgradeHandler underTest;

    @Mock
    private ClusterManagerUpgradeManagementService clusterManagerUpgradeManagementService;

    @Mock
    private ClusterUpgradeService clusterUpgradeService;

    @Test
    void testDoAcceptShouldReturnSuccessEvent() throws CloudbreakOrchestratorException, CloudbreakException {
        ClusterManagerUpgradeRequest request = new ClusterManagerUpgradeRequest(STACK_ID, Collections.emptySet(), true, null);
        when(clusterUpgradeService.isRuntimeUpgradeNecessary(request.getUpgradeCandidateProducts())).thenReturn(false);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT.event(), result.selector());
        verify(clusterManagerUpgradeManagementService).upgradeClusterManager(STACK_ID, true, false, null);
    }

    @Test
    void testDoAcceptShouldReturnFailureEvent() throws CloudbreakOrchestratorException, CloudbreakException {
        ClusterManagerUpgradeRequest request = new ClusterManagerUpgradeRequest(STACK_ID, Collections.emptySet(), true, "123");
        when(clusterUpgradeService.isRuntimeUpgradeNecessary(request.getUpgradeCandidateProducts())).thenReturn(true);
        doThrow(new CloudbreakException("error")).when(clusterManagerUpgradeManagementService).upgradeClusterManager(STACK_ID, true, true, "123");

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FAILED_EVENT.event(), result.selector());
        verify(clusterManagerUpgradeManagementService).upgradeClusterManager(STACK_ID, true, true, "123");
    }
}