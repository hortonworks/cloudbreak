package com.sequenceiq.freeipa.service.proxy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    private static final InstanceMetaData I_1 = createInstance(1L, true);

    private static final InstanceMetaData I_2 = createInstance(2L, false);

    private static final InstanceMetaData I_3 = createInstance(3L, false);

    @Mock
    private StackService stackService;

    @Mock
    private OrchestratorParamsProvider orchestratorParamsProvider;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private FreeIpaSafeInstanceHealthDetailsService healthDetailsService;

    @InjectMocks
    private ModifyProxyConfigOrchestratorService underTest;

    @Mock
    private OrchestratorStateParams stateParams1;

    @Mock
    private OrchestratorStateParams stateParams2;

    @Mock
    private OrchestratorStateParams stateParams3;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        lenient().when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(I_2, I_1, I_3));
        lenient().when(orchestratorParamsProvider.createStateParamsForSingleTarget(eq(stack), eq(I_1.getDiscoveryFQDN()), any())).thenReturn(stateParams1);
        lenient().when(orchestratorParamsProvider.createStateParamsForSingleTarget(eq(stack), eq(I_2.getDiscoveryFQDN()), any())).thenReturn(stateParams2);
        lenient().when(orchestratorParamsProvider.createStateParamsForSingleTarget(eq(stack), eq(I_3.getDiscoveryFQDN()), any())).thenReturn(stateParams3);
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setStatus(InstanceStatus.CREATED);
        lenient().when(healthDetailsService.getInstanceHealthDetails(eq(stack), any())).thenReturn(nodeHealthDetails);
    }

    private static InstanceMetaData createInstance(long privateId, boolean primaryGateway) {
        InstanceMetaData instance = new InstanceMetaData();
        instance.setPrivateId(privateId);
        instance.setDiscoveryFQDN("fqdn-" + privateId);
        instance.setInstanceId("id-" + privateId);
        instance.setInstanceMetadataType(primaryGateway ? InstanceMetadataType.GATEWAY_PRIMARY : InstanceMetadataType.GATEWAY);
        return instance;
    }

    @Test
    void applyModifyProxyState() throws Exception {
        underTest.applyModifyProxyState(STACK_ID);

        InOrder inOrder = inOrder(orchestratorParamsProvider, hostOrchestrator, healthDetailsService);
        inOrder.verify(orchestratorParamsProvider)
                .createStateParamsForSingleTarget(stack, I_2.getDiscoveryFQDN(), ModifyProxyConfigOrchestratorService.MODIFY_PROXY_STATE);
        inOrder.verify(hostOrchestrator).runOrchestratorState(stateParams2);
        inOrder.verify(healthDetailsService).getInstanceHealthDetails(stack, I_2);
        inOrder.verify(orchestratorParamsProvider)
                .createStateParamsForSingleTarget(stack, I_3.getDiscoveryFQDN(), ModifyProxyConfigOrchestratorService.MODIFY_PROXY_STATE);
        inOrder.verify(hostOrchestrator).runOrchestratorState(stateParams3);
        inOrder.verify(healthDetailsService).getInstanceHealthDetails(stack, I_3);
        inOrder.verify(orchestratorParamsProvider)
                .createStateParamsForSingleTarget(stack, I_1.getDiscoveryFQDN(), ModifyProxyConfigOrchestratorService.MODIFY_PROXY_STATE);
        inOrder.verify(hostOrchestrator).runOrchestratorState(stateParams1);
        inOrder.verify(healthDetailsService).getInstanceHealthDetails(stack, I_1);
    }

    @Test
    void applyModifyProxyStateHealthCheckFails() throws Exception {
        NodeHealthDetails nodeHealthDetails = mock(NodeHealthDetails.class);
        String issue = "cause";
        when(nodeHealthDetails.getIssues()).thenReturn(List.of(issue));
        when(nodeHealthDetails.getStatus()).thenReturn(InstanceStatus.FAILED);
        when(healthDetailsService.getInstanceHealthDetails(stack, I_2)).thenReturn(nodeHealthDetails);

        Assertions.assertThatThrownBy(() -> underTest.applyModifyProxyState(STACK_ID))
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasMessage("Health check failed on instance %s after proxy configuration modification. " +
                                "Please either fix your proxy configuration settings and try the operation again, or repair the failed instance. Details: %s",
                        I_2.getInstanceId(), issue);

        verify(orchestratorParamsProvider)
                .createStateParamsForSingleTarget(stack, I_2.getDiscoveryFQDN(), ModifyProxyConfigOrchestratorService.MODIFY_PROXY_STATE);
        verify(hostOrchestrator).runOrchestratorState(stateParams2);
        verify(healthDetailsService).getInstanceHealthDetails(stack, I_2);

        verify(orchestratorParamsProvider, never())
                .createStateParamsForSingleTarget(stack, I_3.getDiscoveryFQDN(), ModifyProxyConfigOrchestratorService.MODIFY_PROXY_STATE);
        verify(hostOrchestrator, never()).runOrchestratorState(stateParams3);
        verify(healthDetailsService, never()).getInstanceHealthDetails(stack, I_3);
        verify(orchestratorParamsProvider, never())
                .createStateParamsForSingleTarget(stack, I_1.getDiscoveryFQDN(), ModifyProxyConfigOrchestratorService.MODIFY_PROXY_STATE);
        verify(hostOrchestrator, never()).runOrchestratorState(stateParams1);
        verify(healthDetailsService, never()).getInstanceHealthDetails(stack, I_1);
    }

}
