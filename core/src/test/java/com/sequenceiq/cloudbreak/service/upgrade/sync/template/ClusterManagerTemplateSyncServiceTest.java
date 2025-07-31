package com.sequenceiq.cloudbreak.service.upgrade.sync.template;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterManagerTemplateSyncServiceTest {

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private ClusterApi clusterApiConnectors;

    @Mock
    private StackDtoService stackService;

    @InjectMocks
    private ClusterManagerTemplateSyncService service;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    void setup() {
        stack = mock(Stack.class);
        cluster = mock(Cluster.class);

        when(stack.getName()).thenReturn("test-stack");
        when(stack.getResourceCrn()).thenReturn("crn:cdp:datalake:us-west-1:acc-123:datalake:cluster1");

        when(stackService.getStackReferenceById(anyLong())).thenReturn(stack);
    }

    @Test
    void testPersistTemplateEntitlementEnabledWithValidDeployment() {
        when(entitlementService.isCdpCBCmTemplateSyncEnabled("acc-123")).thenReturn(true);
        when(clusterStatusService.getDeployment("test-stack")).thenReturn("deployment-template");
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(42L);
        when(clusterApiConnectors.clusterStatusService()).thenReturn(clusterStatusService);
        when(apiConnectors.getConnector(stack)).thenReturn(clusterApiConnectors);

        service.sync(1L);

        verify(clusterService).updateExtendedBlueprintText(42L, "deployment-template");
    }

    @Test
    void testPersistTemplateEntitlementEnabledWithBlankDeployment() {
        when(entitlementService.isCdpCBCmTemplateSyncEnabled("acc-123")).thenReturn(true);
        when(clusterStatusService.getDeployment("test-stack")).thenReturn("");
        when(clusterApiConnectors.clusterStatusService()).thenReturn(clusterStatusService);
        when(apiConnectors.getConnector(stack)).thenReturn(clusterApiConnectors);

        service.sync(1L);

        verify(clusterService, never()).updateExtendedBlueprintText(anyLong(), anyString());
    }

    @Test
    void testPersistTemplateEntitlementDisabled() {
        when(entitlementService.isCdpCBCmTemplateSyncEnabled("acc-123")).thenReturn(false);

        service.sync(1L);

        verify(clusterService, never()).updateExtendedBlueprintText(anyLong(), anyString());
        verifyNoInteractions(clusterStatusService);
    }
}