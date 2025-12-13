package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RecipeAttachmentCheckerTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private RecipeAttachmentChecker recipeAttachmentChecker;

    @Test
    void testIsRecipeAttachmentAvailableIfVersionIsGreater() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        stack.setAppVersion("2.60.0-b11");
        when(stackService.getStackById(1L)).thenReturn(stack);
        recipeAttachmentChecker.isRecipeAttachmentAvailable(1L);

        Stack stack2 = new Stack();
        stack2.setAppVersion("2.59.0-b22");
        when(stackService.getStackById(2L)).thenReturn(stack2);
        recipeAttachmentChecker.isRecipeAttachmentAvailable(2L);

        verify(gatewayConfigService, times(0)).getPrimaryGatewayConfig(any());
        verify(hostOrchestrator, times(0)).doesPhaseSlsExistWithTimeouts(any(), any(), anyInt(), anyInt());
    }

    @Test
    void testIsRecipeAttachmentAvailableIfVersionIsLower() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        StackStatus stackStatus = mock(StackStatus.class);
        when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setAppVersion("2.58.0-b14");
        when(stackService.getStackById(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt())).thenReturn(true);
        boolean recipeAttachmentAvailable = recipeAttachmentChecker.isRecipeAttachmentAvailable(1L);
        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(any());
        verify(hostOrchestrator, times(1)).doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt());
        assertTrue(recipeAttachmentAvailable);
    }

    @Test
    void testIsRecipeAttachmentAvailableIfVersionIsLowerAndSlsMissing() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        StackStatus stackStatus = mock(StackStatus.class);
        when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setAppVersion("2.58.0-b14");
        when(stackService.getStackById(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt())).thenReturn(false);
        boolean recipeAttachmentAvailable = recipeAttachmentChecker.isRecipeAttachmentAvailable(1L);
        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(any());
        verify(hostOrchestrator, times(1)).doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt());
        assertFalse(recipeAttachmentAvailable);
    }

    @Test
    void testIsRecipeAttachmentAvailableIfVersionIsLowerButSlsCheckFails() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        StackStatus stackStatus = mock(StackStatus.class);
        when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setAppVersion("2.58.0-b14");
        when(stackService.getStackById(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt()))
                .thenThrow(new CloudbreakOrchestratorFailedException("error"));
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> recipeAttachmentChecker.isRecipeAttachmentAvailable(1L));
        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(any());
        verify(hostOrchestrator, times(1)).doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt());
        assertEquals("We can not check if your FreeIPA supports recipes: error", badRequestException.getMessage());
    }

    @Test
    void testIsRecipeAttachmentAvailableIfVersionIsLowerButStackNotAvailable() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        StackStatus stackStatus = mock(StackStatus.class);
        when(stackStatus.getStatus()).thenReturn(Status.UNHEALTHY);
        stack.setStackStatus(stackStatus);
        stack.setAppVersion("2.58.0-b14");
        when(stackService.getStackById(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> recipeAttachmentChecker.isRecipeAttachmentAvailable(1L));
        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(any());
        verify(hostOrchestrator, times(0)).doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq("recipes.runner"), anyInt(), anyInt());
        assertEquals("We can not check if your FreeIPA supports recipes because it is not in available state", badRequestException.getMessage());
    }
}