package com.sequenceiq.cloudbreak.service.orchestrator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class OrchestratorServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private OrchestratorService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @BeforeEach
    public void setUp() {
        underTest = new OrchestratorService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStoredStates() {
        // GIVEN
        given(stackService.getById(STACK_ID)).willReturn(stack());
        given(clusterComponentConfigProvider.getSaltStateComponent(STACK_ID)).willReturn(new byte[0]);
        // WHEN
        underTest.getStoredStates(STACK_ID);
        // THEN
        verify(clusterComponentConfigProvider, times(1)).getSaltStateComponent(STACK_ID);
    }

    @Test
    public void testGetOrchestratorMetadata() {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(stack());
        given(gatewayConfigService.getAllGatewayConfigs(any())).willReturn(new ArrayList<>());
        // WHEN
        OrchestratorMetadata result = underTest.getOrchestratorMetadata(STACK_ID);
        // THEN
        assertNotNull(result);
        verify(gatewayConfigService, times(1)).getAllGatewayConfigs(any());
    }

    @Test
    public void testStoreNewState() {
        // GIVEN
        given(stackService.getById(STACK_ID)).willReturn(stack());
        given(clusterBootstrapper.updateSaltComponent(any(), any())).willReturn(new ClusterComponent());
        // WHEN
        underTest.storeNewState(STACK_ID, new byte[0]);
        // THEN
        verify(clusterBootstrapper, times(1)).updateSaltComponent(any(), any());
    }

    @Test
    public void testGetSaltStateDefinitionBaseFolders() {
        // GIVEN
        // WHEN
        List<String> result = underTest.getSaltStateDefinitionBaseFolders();
        // THEN
        assertTrue(result.contains("salt"));
        assertTrue(result.contains("salt-common"));
    }

    private Stack stack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setId(STACK_ID);
        stack.setCluster(cluster);
        return stack;
    }
}
