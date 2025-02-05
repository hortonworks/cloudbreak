package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaInstallServiceTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private FreeIpaRecipeService freeIpaRecipeService;

    @InjectMocks
    private FreeIpaInstallService freeIpaInstallService;

    @Test
    public void testInstall() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceGroup masterInstanceGroup = new InstanceGroup();
        masterInstanceGroup.setGroupName("master");
        when(stack.getInstanceGroups()).thenReturn(Set.of(masterInstanceGroup));
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(new InstanceMetaData());
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        List<GatewayConfig> allGatewayConfigs = List.of(gatewayConfig);
        when(gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet)).thenReturn(allGatewayConfigs);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);
        Node node = mock(Node.class);
        Set<Node> nodes = Set.of(node);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        List<RecipeModel> recipeModelList = List.of(new RecipeModel("recipe1", RecipeType.PRE_SERVICE_DEPLOYMENT, "bash"),
                new RecipeModel("recipe1", RecipeType.PRE_TERMINATION, "bash"));
        when(freeIpaRecipeService.getRecipes(1L)).thenReturn(recipeModelList);
        when(freeIpaRecipeService.hasRecipeType(recipeModelList, RecipeType.PRE_SERVICE_DEPLOYMENT, RecipeType.PRE_CLOUDERA_MANAGER_START)).thenReturn(true);
        freeIpaInstallService.installFreeIpa(1L);
        verify(hostOrchestrator).uploadRecipes(eq(allGatewayConfigs), eq(Map.of("master", recipeModelList)), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator).preServiceDeploymentRecipes(eq(gatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator).installFreeIpa(eq(gatewayConfig), eq(allGatewayConfigs), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator, never()).postClusterManagerStartRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
    }

    @Test
    public void testInstallButNoPreRecipe() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(new InstanceMetaData());
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        List<GatewayConfig> allGatewayConfigs = List.of(gatewayConfig);
        when(gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet)).thenReturn(allGatewayConfigs);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);
        Node node = mock(Node.class);
        Set<Node> nodes = Set.of(node);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        List<RecipeModel> recipeModelList = List.of(new RecipeModel("recipe1", RecipeType.PRE_CLOUDERA_MANAGER_START, "bash"),
                new RecipeModel("recipe1", RecipeType.PRE_TERMINATION, "bash"));
        when(freeIpaRecipeService.getRecipes(1L)).thenReturn(recipeModelList);
        when(freeIpaRecipeService.hasRecipeType(recipeModelList, RecipeType.PRE_SERVICE_DEPLOYMENT, RecipeType.PRE_CLOUDERA_MANAGER_START)).thenReturn(false);
        freeIpaInstallService.installFreeIpa(1L);
        verify(hostOrchestrator, times(1)).uploadRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator, times(0)).preServiceDeploymentRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator).installFreeIpa(eq(gatewayConfig), eq(allGatewayConfigs), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator, never()).postClusterManagerStartRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
    }

    @Test
    public void testInstallButNoRecipe() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(new InstanceMetaData());
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        List<GatewayConfig> allGatewayConfigs = List.of(gatewayConfig);
        when(gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet)).thenReturn(allGatewayConfigs);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stack)).thenReturn(gatewayConfig);
        Node node = mock(Node.class);
        Set<Node> nodes = Set.of(node);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(freeIpaRecipeService.getRecipes(1L)).thenReturn(Collections.emptyList());
        freeIpaInstallService.installFreeIpa(1L);
        verify(hostOrchestrator, times(0)).uploadRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator, times(0)).preServiceDeploymentRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator).installFreeIpa(eq(gatewayConfig), eq(allGatewayConfigs), eq(nodes), any(StackBasedExitCriteriaModel.class));
        verify(hostOrchestrator, never()).postClusterManagerStartRecipes(any(), any(), any(StackBasedExitCriteriaModel.class));
    }

}