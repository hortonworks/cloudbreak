package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.recipes.ExecutePreTerminationRecipesFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.recipes.ExecutePreTerminationRecipesRequest;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class ExecutePreTerminationRecipesHandlerTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private ExecutePreTerminationRecipesHandler executePreTerminationRecipesHandler;

    @Test
    public void doAcceptTest() throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelArgumentCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        ExecutePreTerminationRecipesRequest executePreTerminationRecipesRequest = new ExecutePreTerminationRecipesRequest(1L, true);
        Selectable selectable = executePreTerminationRecipesHandler.doAccept(new HandlerEvent<>(new Event<>(executePreTerminationRecipesRequest)));
        verify(hostOrchestrator).preTerminationRecipes(eq(gatewayConfig), eq(nodes), exitCriteriaModelArgumentCaptor.capture(), eq(true));
        ExitCriteriaModel exitCriteriaModel = exitCriteriaModelArgumentCaptor.getValue();
        assertTrue(((StackBasedExitCriteriaModel) exitCriteriaModel).getStackId().isEmpty());
        assertEquals(ExecutePreTerminationRecipesFinished.class, selectable.getClass());
        assertTrue(((ExecutePreTerminationRecipesFinished) selectable).getForced());
        assertEquals(1L, selectable.getResourceId());
    }

    @Test
    public void doAcceptTestWhenCloudbreakOrchestratorFailedExceptionHappen() throws CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        ExecutePreTerminationRecipesRequest executePreTerminationRecipesRequest = new ExecutePreTerminationRecipesRequest(1L, false);
        doThrow(new CloudbreakOrchestratorFailedException("exception"))
                .when(hostOrchestrator).preTerminationRecipes(eq(gatewayConfig), eq(nodes), any(), anyBoolean());
        Selectable selectable = executePreTerminationRecipesHandler.doAccept(new HandlerEvent<>(new Event<>(executePreTerminationRecipesRequest)));
        assertEquals(StackFailureEvent.class, selectable.getClass());
        Exception exception = ((StackFailureEvent) selectable).getException();
        assertEquals("exception", exception.getMessage());
    }

    @Test
    public void doAcceptTestWhenCloudbreakOrchestratorTimeoutExceptionHappen() throws CloudbreakOrchestratorFailedException,
            CloudbreakOrchestratorTimeoutException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        Set<Node> nodes = Set.of(mock(Node.class));
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        ExecutePreTerminationRecipesRequest executePreTerminationRecipesRequest = new ExecutePreTerminationRecipesRequest(1L, false);
        doThrow(new CloudbreakOrchestratorTimeoutException("timeoutException", 100L))
                .when(hostOrchestrator).preTerminationRecipes(eq(gatewayConfig), eq(nodes), any(), anyBoolean());
        Selectable selectable = executePreTerminationRecipesHandler.doAccept(new HandlerEvent<>(new Event<>(executePreTerminationRecipesRequest)));
        assertEquals(StackFailureEvent.class, selectable.getClass());
        Exception exception = ((StackFailureEvent) selectable).getException();
        assertEquals("timeoutException", exception.getMessage());
    }

    @Test
    public void doAcceptTestButEveryInstanceIsDeleted() throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> instanceMetaDataSet = Collections.emptySet();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        ExecutePreTerminationRecipesRequest executePreTerminationRecipesRequest = new ExecutePreTerminationRecipesRequest(1L, true);
        Selectable selectable = executePreTerminationRecipesHandler.doAccept(new HandlerEvent<>(new Event<>(executePreTerminationRecipesRequest)));
        verify(hostOrchestrator, times(0)).preTerminationRecipes(any(), any(), any(), anyBoolean());
        assertEquals(ExecutePreTerminationRecipesFinished.class, selectable.getClass());
        assertTrue(((ExecutePreTerminationRecipesFinished) selectable).getForced());
        assertEquals(1L, selectable.getResourceId());
    }

    @Test
    public void doAcceptTestButWeHaveDeletedAndStoppedNodesOnly() throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceStatus(InstanceStatus.STOPPED);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData1, instanceMetaData2);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        ExecutePreTerminationRecipesRequest executePreTerminationRecipesRequest = new ExecutePreTerminationRecipesRequest(1L, true);
        Selectable selectable = executePreTerminationRecipesHandler.doAccept(new HandlerEvent<>(new Event<>(executePreTerminationRecipesRequest)));
        verify(hostOrchestrator, times(0)).preTerminationRecipes(any(), any(), any(), anyBoolean());
        assertEquals(ExecutePreTerminationRecipesFinished.class, selectable.getClass());
        assertTrue(((ExecutePreTerminationRecipesFinished) selectable).getForced());
        assertEquals(1L, selectable.getResourceId());
    }

    @Test
    public void doAcceptTestButWeHaveDeleted1StoppedAnd1CreatedNode() throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorTimeoutException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceStatus(InstanceStatus.CREATED);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData1, instanceMetaData2);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        Set<Node> nodes = Set.of(mock(Node.class));
        ArgumentCaptor<Set<InstanceMetaData>> instanceMetadatasArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetadatasArgumentCaptor.capture())).thenReturn(nodes);
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelArgumentCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        ExecutePreTerminationRecipesRequest executePreTerminationRecipesRequest = new ExecutePreTerminationRecipesRequest(1L, true);
        Selectable selectable = executePreTerminationRecipesHandler.doAccept(new HandlerEvent<>(new Event<>(executePreTerminationRecipesRequest)));
        verify(hostOrchestrator).preTerminationRecipes(eq(gatewayConfig), eq(nodes), exitCriteriaModelArgumentCaptor.capture(), eq(true));
        Set<InstanceMetaData> instanceMetadatasArgumentCaptorValue = instanceMetadatasArgumentCaptor.getValue();
        assertThat(instanceMetadatasArgumentCaptorValue).containsExactly(instanceMetaData2);
        ExitCriteriaModel exitCriteriaModel = exitCriteriaModelArgumentCaptor.getValue();
        assertTrue(((StackBasedExitCriteriaModel) exitCriteriaModel).getStackId().isEmpty());
        assertEquals(ExecutePreTerminationRecipesFinished.class, selectable.getClass());
        assertTrue(((ExecutePreTerminationRecipesFinished) selectable).getForced());
        assertEquals(1L, selectable.getResourceId());
    }

}