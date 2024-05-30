package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreSuccess;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaRestoreHandlerTest {

    private static final long RESOURCE_ID = 3L;

    private static final String HOSTNAME = "host1";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @Captor
    private ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor;

    @InjectMocks
    private FreeIpaRestoreHandler underTest;

    @Test
    void selector() {
        assertEquals(EventSelectorUtil.selector(FreeIpaRestoreRequest.class), underTest.selector());
    }

    @Test
    void defaultFailureEvent() {
        Exception e = new Exception("sdfg");

        FreeIpaRestoreFailed result = (FreeIpaRestoreFailed)
                underTest.defaultFailureEvent(RESOURCE_ID, e, new Event<>(new FreeIpaRestoreRequest(RESOURCE_ID)));

        assertEquals(RESOURCE_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void doAcceptSuccess() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(stack.getId()).thenReturn(RESOURCE_ID);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        FreeIpaRestoreSuccess result = (FreeIpaRestoreSuccess) underTest.doAccept(new HandlerEvent<>(new Event<>(new FreeIpaRestoreRequest(RESOURCE_ID))));

        assertEquals(RESOURCE_ID, result.getResourceId());
        verify(stackService).getByIdWithListsInTransaction(RESOURCE_ID);
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams capturedParams = orchestratorStateParamsCaptor.getValue();
        assertNotNull(capturedParams);
        assertEquals(gatewayConfig, capturedParams.getPrimaryGatewayConfig());
        assertEquals(Set.of(HOSTNAME), capturedParams.getTargetHostNames());
        assertEquals("freeipa/rebuild/restore", capturedParams.getState());
        assertEquals(new StackBasedExitCriteriaModel(RESOURCE_ID), capturedParams.getExitCriteriaModel());
    }

    @Test
    void doAcceptFailure() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(stack.getId()).thenReturn(RESOURCE_ID);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        doThrow(new CloudbreakOrchestratorFailedException("Error")).when(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));

        FreeIpaRestoreFailed result = (FreeIpaRestoreFailed) underTest.doAccept(new HandlerEvent<>(new Event<>(new FreeIpaRestoreRequest(RESOURCE_ID))));

        assertEquals(RESOURCE_ID, result.getResourceId());
        verify(stackService).getByIdWithListsInTransaction(RESOURCE_ID);
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams capturedParams = orchestratorStateParamsCaptor.getValue();
        assertNotNull(capturedParams);
        assertEquals(gatewayConfig, capturedParams.getPrimaryGatewayConfig());
        assertEquals(Set.of(HOSTNAME), capturedParams.getTargetHostNames());
        assertEquals("freeipa/rebuild/restore", capturedParams.getState());
        assertEquals(new StackBasedExitCriteriaModel(RESOURCE_ID), capturedParams.getExitCriteriaModel());
    }
}