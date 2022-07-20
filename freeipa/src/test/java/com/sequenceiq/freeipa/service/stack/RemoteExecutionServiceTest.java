package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionRequest;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionResponse;
import com.sequenceiq.freeipa.controller.exception.UnsupportedException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorService;

@ExtendWith(MockitoExtension.class)
public class RemoteExecutionServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private RemoteExecutionService underTest;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private OrchestratorService orchestratorService;

    @Mock
    private StackService stackService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new RemoteExecutionService(stackService, hostOrchestrator, orchestratorService, true);
    }

    @Test
    public void testRemoteExec() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        Map<String, String> result = new HashMap<>();
        result.put("host1", "sample");
        RemoteCommandsExecutionRequest request = new RemoteCommandsExecutionRequest();
        String command = "echo sample";
        request.setCommand(command);
        List<GatewayConfig> gatewayConfigList = new ArrayList<>();
        Set<Node> nodes = new HashSet<>();
        OrchestratorMetadata metadata = new OrchestratorMetadata(gatewayConfigList, nodes, null, null);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        given(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).willReturn(stack);
        given(orchestratorService.getOrchestratorMetadata(anyLong())).willReturn(metadata);
        given(hostOrchestrator.runCommandOnHosts(gatewayConfigList, nodes, command)).willReturn(result);
        // WHEN
        RemoteCommandsExecutionResponse response = underTest.remoteExec(ENVIRONMENT_CRN, request);
        // THEN
        assertEquals("sample", response.getResults().get("host1"));
        verify(hostOrchestrator, times(1)).runCommandOnHosts(gatewayConfigList, nodes, command);
    }

    @Test
    public void testRemoteExecWithOrchestrationError() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        RemoteCommandsExecutionRequest request = new RemoteCommandsExecutionRequest();
        String command = "echo sample";
        request.setCommand(command);
        List<GatewayConfig> gatewayConfigList = new ArrayList<>();
        Set<Node> nodes = new HashSet<>();
        OrchestratorMetadata metadata = new OrchestratorMetadata(gatewayConfigList, nodes, null, null);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        given(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).willReturn(stack);
        given(orchestratorService.getOrchestratorMetadata(anyLong())).willReturn(metadata);
        given(hostOrchestrator.runCommandOnHosts(gatewayConfigList, nodes, command)).willThrow(new CloudbreakOrchestratorFailedException("ex"));
        // WHEN
        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class,
                () -> underTest.remoteExec(ENVIRONMENT_CRN, request));
        // THEN
        assertTrue(ex.getMessage().contains("ex"));
    }

    @Test
    public void testRemoteExecIsNotSupported() {
        // GIVEN
        underTest = new RemoteExecutionService(stackService, hostOrchestrator, orchestratorService, false);
        // WHEN
        UnsupportedException ex = assertThrows(UnsupportedException.class,
                () -> underTest.remoteExec(ENVIRONMENT_CRN, new RemoteCommandsExecutionRequest()));
        // THEN
        assertEquals("Remote command execution is not supported!", ex.getMessage());
    }
}
