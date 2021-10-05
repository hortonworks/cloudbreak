package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.recipe.GeneratedRecipeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class OrchestratorRecipeExecutorTest {

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

    @Captor
    private ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelCaptor;

    @Test
    public void preClusterManagerStartRecipesShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        when(stack.getId()).thenReturn(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stack.getCluster()).thenReturn(cluster);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.preClusterManagerStartRecipes(stack);

        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator).preClusterManagerStartRecipes(eq(gatewayConfig), eq(nodes), any());
    }

    @Test
    public void postClusterManagerStartRecipesShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        when(stack.getId()).thenReturn(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stack.getCluster()).thenReturn(cluster);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(this.node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.postClusterManagerStartRecipes(stack);

        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator).postClusterManagerStartRecipes(eq(gatewayConfig), eq(nodes), any());
    }

    @Test
    public void postClusterInstallShouldUseReachableNodes() throws CloudbreakException, CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        when(stack.getId()).thenReturn(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stack.getCluster()).thenReturn(cluster);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(this.node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.postClusterInstall(stack);

        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator).postInstallRecipes(eq(gatewayConfig), eq(nodes), any());
    }

    @Test
    public void testPreTerminationRecipes() throws CloudbreakException, CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        Set<Node> nodes = Set.of(node);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);

        underTest.preTerminationRecipes(stack, false);

        verify(hostOrchestrator).preTerminationRecipes(eq(gatewayConfig), eq(Set.of(node)), exitCriteriaModelCaptor.capture(), eq(false));

        ExitCriteriaModel exitCriteriaModel = exitCriteriaModelCaptor.getValue();
        assertThat(exitCriteriaModel).isOfAnyClassIn(ClusterDeletionBasedExitCriteriaModel.class);
    }

    @Test
    public void testGetSingleRecipeExecutionFailureMessageWithInstanceMetaData() {
        final InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        final InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("instance-group");
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(recipeExecutionFailureCollector.getInstanceMetadataByHost(anySet(), anyString())).thenReturn(Optional.of(instanceMetaData));

        final RecipeExecutionFailureCollector.RecipeFailure recipeFailure = new RecipeExecutionFailureCollector.RecipeFailure("fqdn", "phase", "recipe");
        final String message = underTest.getSingleRecipeExecutionFailureMessage(Set.of(instanceMetaData), recipeFailure);

        Assert.assertEquals("[Recipe: 'recipe' - \nHostgroup: 'instance-group' - \nInstance: 'fqdn']", message);
    }

    @Test
    public void testGetSingleRecipeExecutionFailureMessageWithoutInstanceMetaData() {
        final InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("other-fqdn");
        final InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("instance-group");
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(recipeExecutionFailureCollector.getInstanceMetadataByHost(anySet(), anyString())).thenReturn(Optional.empty());

        final RecipeExecutionFailureCollector.RecipeFailure recipeFailure = new RecipeExecutionFailureCollector.RecipeFailure("fqdn", "phase", "recipe");
        final String message = underTest.getSingleRecipeExecutionFailureMessage(Set.of(instanceMetaData), recipeFailure);

        Assert.assertEquals("[Recipe: 'recipe' - \nInstance: 'fqdn' (missing metadata)]", message);
    }
}