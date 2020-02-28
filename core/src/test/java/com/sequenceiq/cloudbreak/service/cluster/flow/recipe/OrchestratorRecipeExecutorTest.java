package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.recipe.CentralRecipeUpdater;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.recipe.GeneratedRecipeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class OrchestratorRecipeExecutorTest {

    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private GeneratedRecipeService generatedRecipeService;

    @Mock
    private RecipeExecutionFailureCollector recipeExecutionFailureCollector;

    @Mock
    private CentralRecipeUpdater centralRecipeUpdater;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private OrchestratorRecipeExecutor underTest;

    @Mock
    private Stack stack;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private Node node;

    @Test
    public void preClusterManagerStartRecipesShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        when(stack.getId()).thenReturn(1L);
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("ORCHESTRATOR_TYPE");
        when(stack.getOrchestrator()).thenReturn(orchestrator);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stack.getCluster()).thenReturn(cluster);
        when(hostOrchestratorResolver.get(anyString())).thenReturn(hostOrchestrator);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(this.node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.preClusterManagerStartRecipes(stack);

        verify(hostOrchestratorResolver).get("ORCHESTRATOR_TYPE");
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator).preClusterManagerStartRecipes(eq(gatewayConfig), eq(nodes), any());
    }

    @Test
    public void postClusterManagerStartRecipesShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        when(stack.getId()).thenReturn(1L);
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("ORCHESTRATOR_TYPE");
        when(stack.getOrchestrator()).thenReturn(orchestrator);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stack.getCluster()).thenReturn(cluster);
        when(hostOrchestratorResolver.get(anyString())).thenReturn(hostOrchestrator);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(this.node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.postClusterManagerStartRecipes(stack);

        verify(hostOrchestratorResolver).get("ORCHESTRATOR_TYPE");
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator).postClusterManagerStartRecipes(eq(gatewayConfig), eq(nodes), any());
    }

    @Test
    public void postClusterInstallShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        when(stack.getId()).thenReturn(1L);
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("ORCHESTRATOR_TYPE");
        when(stack.getOrchestrator()).thenReturn(orchestrator);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stack.getCluster()).thenReturn(cluster);
        when(hostOrchestratorResolver.get(anyString())).thenReturn(hostOrchestrator);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(this.node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.postClusterInstall(stack);

        verify(hostOrchestratorResolver).get("ORCHESTRATOR_TYPE");
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator).postInstallRecipes(eq(gatewayConfig), eq(nodes), any());
    }
}