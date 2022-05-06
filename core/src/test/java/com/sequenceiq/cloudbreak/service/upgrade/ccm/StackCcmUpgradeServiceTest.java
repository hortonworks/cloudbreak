package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTriggerRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class StackCcmUpgradeServiceTest {

    private static final Long CLUSTER_ID = 123L;

    private static final Long STACK_ID = 234L;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackService stackService;

    @Mock
    private ReactorNotifier reactorNotifier;

    @InjectMocks
    private StackCcmUpgradeService underTest;

    @Test
    void testUpgradeCcm() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(Tunnel.CCMV2);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        when(stackService.getByNameOrCrnInWorkspace(any(), any())).thenReturn(stack);
        underTest.upgradeCcm(NameOrCrn.ofName("name"));
        ArgumentCaptor<UpgradeCcmTriggerRequest> requestCaptor = ArgumentCaptor.forClass(UpgradeCcmTriggerRequest.class);
        verify(reactorNotifier).notify(eq(STACK_ID), eq("UPGRADE_CCM_TRIGGER_EVENT"), requestCaptor.capture());
        UpgradeCcmTriggerRequest request = requestCaptor.getValue();
        assertThat(request.getResourceId()).isEqualTo(STACK_ID);
        assertThat(request.getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(request.getOldTunnel()).isEqualTo(Tunnel.CCMV2);
    }
}
