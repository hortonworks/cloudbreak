package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_COLLECT_UNREACHABLE_FOUND_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MASTER_SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MASTER_SERVICES_UNHEALTHY_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MINIONS_UNREACHABLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SALT_MINIONS_UNREACHABLE_DETAILS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.HealthStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.SaltHealthReport;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.SaltMasterHealth;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.SaltMinionsHealth;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServiceStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.StatusDetails;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class SaltCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private SaltCheckerConclusionStep underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Test
    public void checkShouldFallbackIfNodeStatusCheckFailsAndBeSuccessfulIfNoUnreachableNodeFound() throws NodesUnreachableException {
        when(nodeStatusService.saltPing(eq(1L))).thenThrow(new CloudbreakServiceException("error"));
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        Set<Node> nodes = Set.of(createNode("host1"), createNode("host2"));
        when(stackUtil.collectNodes(any(), any())).thenReturn(nodes);
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), anyCollection())).thenReturn(nodes);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
        verify(stackDtoService, times(1)).getById(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
    }

    @Test
    public void checkShouldFallbackForOldImageVersionsAndBeSuccessfulIfNoUnreachableNodeFound() throws NodesUnreachableException {
        RPCResponse<SaltHealthReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(response);
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        Set<Node> nodes = Set.of(createNode("host1"), createNode("host2"));
        when(stackUtil.collectNodes(any(), any())).thenReturn(nodes);
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), anyCollection())).thenReturn(nodes);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
        verify(stackDtoService, times(1)).getById(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
    }

    @Test
    public void checkShouldFallbackForOldImageVersionsAndReturnConclusionIfUnreachableNodeFound() throws NodesUnreachableException {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_COLLECT_UNREACHABLE_FOUND), any())).thenReturn("collect unreachable found");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_COLLECT_UNREACHABLE_FOUND_DETAILS), any())).thenReturn("collect unreachable found details");
        RPCResponse<SaltHealthReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(response);
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        when(stackUtil.collectNodes(any(), any())).thenReturn(Set.of(createNode("host1"), createNode("host2")));
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), anyCollection())).thenThrow(new NodesUnreachableException("error", Set.of("host1")));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("collect unreachable found", stepResult.getConclusion());
        assertEquals("collect unreachable found details", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
        verify(stackDtoService, times(1)).getById(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
    }

    @Test
    public void checkShouldBeSuccessfulIfNoUnreachableNodeFound() {
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(createSaltPingResponse(HealthStatus.OK, HealthStatus.OK));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfUnhealthyServicesOnMasterFound() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MASTER_SERVICES_UNHEALTHY), any())).thenReturn("master error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MASTER_SERVICES_UNHEALTHY_DETAILS), any())).thenReturn("master error details");
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(createSaltPingResponse(HealthStatus.NOK, HealthStatus.OK));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("master error", stepResult.getConclusion());
        assertEquals("master error details", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfUnhealthyMinionsFound() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MINIONS_UNREACHABLE), any())).thenReturn("minions unreachable");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SALT_MINIONS_UNREACHABLE_DETAILS), any())).thenReturn("minions unreachable details");
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(createSaltPingResponse(HealthStatus.OK, HealthStatus.NOK));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("minions unreachable", stepResult.getConclusion());
        assertEquals("minions unreachable details", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
    }

    private RPCResponse<SaltHealthReport> createSaltPingResponse(HealthStatus masterServiceStatus, HealthStatus minionHealthStatus) {
        StatusDetails pingResponses = StatusDetails.newBuilder()
                .setHost("host1")
                .setStatus(minionHealthStatus)
                .setStatusReason("bigproblem")
                .build();
        SaltMinionsHealth saltMinionsHealth = SaltMinionsHealth.newBuilder()
                .addPingResponses(pingResponses)
                .build();
        ServiceStatus serviceStatus = ServiceStatus.newBuilder()
                .setName("salt-bootstrap")
                .setStatus(masterServiceStatus)
                .build();
        SaltMasterHealth saltMasterHealth = SaltMasterHealth.newBuilder()
                .addServices(serviceStatus)
                .build();
        SaltHealthReport saltHealthReport = SaltHealthReport.newBuilder()
                .setMaster(saltMasterHealth)
                .setMinions(saltMinionsHealth)
                .build();
        RPCResponse<SaltHealthReport> response = new RPCResponse<>();
        response.setResult(saltHealthReport);
        return response;
    }

    private Node createNode(String fqdn) {
        return new Node("privateIp", "publicIp", "instanceId", "instanceType",
                fqdn, "hostGroup");
    }

}
