package com.sequenceiq.cloudbreak.conclusion.step;

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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class SaltCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @Mock
    private StackService stackService;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private SaltCheckerConclusionStep underTest;

    @Test
    public void checkShouldFallbackIfNodeStatusCheckFailsAndBeSuccessfulIfNoUnreachableNodeFound() throws NodesUnreachableException {
        when(nodeStatusService.saltPing(eq(1L))).thenThrow(new CloudbreakServiceException("error"));
        when(stackService.getByIdWithListsInTransaction(eq(1L))).thenReturn(new Stack());
        Set<Node> nodes = Set.of(createNode("host1"), createNode("host2"));
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(stackUtil.collectAndCheckReachableNodes(any(), anyCollection())).thenReturn(nodes);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any());
        verify(stackUtil, times(1)).collectAndCheckReachableNodes(any(), any());
    }

    @Test
    public void checkShouldFallbackForOldImageVersionsAndBeSuccessfulIfNoUnreachableNodeFound() throws NodesUnreachableException {
        RPCResponse<SaltHealthReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(response);
        when(stackService.getByIdWithListsInTransaction(eq(1L))).thenReturn(new Stack());
        Set<Node> nodes = Set.of(createNode("host1"), createNode("host2"));
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(stackUtil.collectAndCheckReachableNodes(any(), anyCollection())).thenReturn(nodes);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any());
        verify(stackUtil, times(1)).collectAndCheckReachableNodes(any(), any());
    }

    @Test
    public void checkShouldFallbackForOldImageVersionsAndReturnConclusionIfUnreachableNodeFound() throws NodesUnreachableException {
        RPCResponse<SaltHealthReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(response);
        when(stackService.getByIdWithListsInTransaction(eq(1L))).thenReturn(new Stack());
        when(stackUtil.collectNodes(any())).thenReturn(Set.of(createNode("host1"), createNode("host2")));
        when(stackUtil.collectAndCheckReachableNodes(any(), anyCollection())).thenThrow(new NodesUnreachableException("error", Set.of("host1")));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("Unreachable nodes: [host1]. We detected that cluster members can’t communicate with each other. " +
                "Please validate if all cluster members are available and healthy through your cloud provider.", stepResult.getConclusion());
        assertEquals("Unreachable salt minions: [host1]", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
        verify(stackService, times(1)).getByIdWithListsInTransaction(eq(1L));
        verify(stackUtil, times(1)).collectNodes(any());
        verify(stackUtil, times(1)).collectAndCheckReachableNodes(any(), any());
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
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(createSaltPingResponse(HealthStatus.NOK, HealthStatus.OK));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("There are unhealthy services on master node: [salt-bootstrap]. " +
                        "Please check the instances on your cloud provider for further details.", stepResult.getConclusion());
        assertEquals("Unhealthy services on master: [salt-bootstrap]", stepResult.getDetails());
        assertEquals(SaltCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).saltPing(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfUnhealthyMinionsFound() {
        when(nodeStatusService.saltPing(eq(1L))).thenReturn(createSaltPingResponse(HealthStatus.OK, HealthStatus.NOK));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("Unreachable nodes: [host1]. We detected that cluster members can’t communicate with each other. " +
                "Please validate if all cluster members are available and healthy through your cloud provider.", stepResult.getConclusion());
        assertEquals("Unreachable salt minions: {host1=bigproblem}", stepResult.getDetails());
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
