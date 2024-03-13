package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupSuccess;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class ValidateBackupHandlerTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String FULL_BACKUP_LOCATION = "fullBackupLocation";

    private static final String DATA_BACKUP_LOCATION = "dataBackupLocation";

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
    private ValidateBackupHandler validateBackupHandler;

    @Test
    public void testDoAcceptSuccess() throws Exception {
        ValidateBackupRequest request = new ValidateBackupRequest(RESOURCE_ID, FULL_BACKUP_LOCATION, DATA_BACKUP_LOCATION);
        HandlerEvent<ValidateBackupRequest> event = new HandlerEvent<>(new Event<>(request));

        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(stack.getId()).thenReturn(RESOURCE_ID);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        Selectable result = validateBackupHandler.doAccept(event);

        assertInstanceOf(ValidateBackupSuccess.class, result);
        verify(stackService).getByIdWithListsInTransaction(RESOURCE_ID);
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams capturedParams = orchestratorStateParamsCaptor.getValue();
        assertNotNull(capturedParams);
        assertEquals(gatewayConfig, capturedParams.getPrimaryGatewayConfig());
        assertEquals(Set.of(HOSTNAME), capturedParams.getTargetHostNames());
        assertEquals("freeipa/rebuild/dl_and_validate_backup", capturedParams.getState());
        assertEquals(new StackBasedExitCriteriaModel(RESOURCE_ID), capturedParams.getExitCriteriaModel());
        assertNotNull(capturedParams.getStateParams());
        assertEquals(FULL_BACKUP_LOCATION,
                ((Map<String, Map<String, String>>) capturedParams.getStateParams().get("freeipa")).get("rebuild").get("full_backup_location"));
        assertEquals(DATA_BACKUP_LOCATION,
                ((Map<String, Map<String, String>>) capturedParams.getStateParams().get("freeipa")).get("rebuild").get("data_backup_location"));
    }

    @Test
    public void testDoAcceptFailure() throws Exception {
        ValidateBackupRequest request = new ValidateBackupRequest(RESOURCE_ID, FULL_BACKUP_LOCATION, DATA_BACKUP_LOCATION);
        HandlerEvent<ValidateBackupRequest> event = new HandlerEvent<>(new Event<>(request));

        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(stack.getId()).thenReturn(RESOURCE_ID);
        when(gatewayConfig.getHostname()).thenReturn("host1");

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        doThrow(new CloudbreakOrchestratorFailedException("Error")).when(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));

        Selectable result = validateBackupHandler.doAccept(event);

        assertInstanceOf(ValidateBackupFailed.class, result);
        verify(stackService).getByIdWithListsInTransaction(RESOURCE_ID);
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));
    }
}
