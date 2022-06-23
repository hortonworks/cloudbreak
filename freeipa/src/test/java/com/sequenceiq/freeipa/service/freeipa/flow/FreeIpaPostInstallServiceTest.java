package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaPostInstallServiceTest {

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaTopologyService freeIpaTopologyService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private StackService stackService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private FreeIpaRecipeService freeIpaRecipeService;

    @InjectMocks
    private FreeIpaPostInstallService freeIpaPostInstallService;

    @Test
    public void postInstallFreeIpaExecutePostInstallRecipes() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaNodeUtilService.mapInstancesToNodes(anySet())).thenReturn(nodes);
        when(freeIpaRecipeService.hasRecipeType(1L, RecipeType.POST_CLUSTER_INSTALL)).thenReturn(true);
        freeIpaPostInstallService.postInstallFreeIpa(1L, false);
        verify(hostOrchestrator).postInstallRecipes(eq(gatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));
    }

    @Test
    public void postInstallFreeIpaExecutePostInstallRecipesButNoRecipes() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaRecipeService.hasRecipeType(1L, RecipeType.POST_CLUSTER_INSTALL)).thenReturn(false);
        freeIpaPostInstallService.postInstallFreeIpa(1L, false);
        verify(hostOrchestrator, times(0)).postInstallRecipes(eq(gatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));
    }

}