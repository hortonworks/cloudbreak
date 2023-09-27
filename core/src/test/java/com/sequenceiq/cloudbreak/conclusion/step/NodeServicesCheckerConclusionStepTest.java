package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_FAILED_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_UNREACHABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.HealthStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatusReport;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServiceStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.ServicesDetails;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.StatusDetails;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@ExtendWith(MockitoExtension.class)
class NodeServicesCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private NodeServicesCheckerConclusionStep underTest;

    @Test
    public void checkShouldBeSuccessfulIfNodeStatusReportFailsForOlderImageVersions() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenThrow(new CloudbreakServiceException("error"));
        when(cloudbreakMessagesService.getMessage(NODE_STATUS_MONITOR_UNREACHABLE)).thenReturn("node status monitor unreachable");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("node status monitor unreachable", stepResult.getConclusion());
        assertEquals("error", stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    @Test
    public void checkShouldBeSuccessfulIfNodeStatusReportNullForOlderImageVersions() {
        when(nodeStatusService.getServicesReport(anyLong())).thenReturn(null);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertEquals(null, stepResult.getConclusion());
        assertEquals(null, stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }

    @Test
    public void checkShouldBeSuccessfulIfNodeStatusReportIsNullForOlderImageVersions() {
        RPCResponse<NodeStatusReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(response);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    @Test
    public void checkShouldBeSuccessfulIfNoNodesWithUnhealthyServicesFound() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(createNodeStatusResponse(HealthStatus.OK, HealthStatus.OK));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNodeWithUnhealthyServicesFound() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(createNodeStatusResponse(HealthStatus.NOK, HealthStatus.NOK));
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NODE_STATUS_MONITOR_FAILED), any())).thenReturn("unhealthy nodes");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NODE_STATUS_MONITOR_FAILED_DETAILS), any())).thenReturn("unhealthy nodes details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("unhealthy nodes", stepResult.getConclusion());
        assertEquals("unhealthy nodes details", stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    private RPCResponse<NodeStatusReport> createNodeStatusResponse(HealthStatus saltBootstrapHealthStatus, HealthStatus cmAgentHealthStatus) {
        StatusDetails statusDetails = StatusDetails.newBuilder()
                .setHost("host1")
                .build();
        ServiceStatus saltBootstrapServiceStatus = ServiceStatus.newBuilder()
                .setName("salt-bootstrap")
                .setStatus(saltBootstrapHealthStatus)
                .build();
        ServiceStatus cmAgentServiceStatus = ServiceStatus.newBuilder()
                .setName("cm-agent")
                .setStatus(cmAgentHealthStatus)
                .build();
        ServicesDetails  servicesDetails = ServicesDetails.newBuilder()
                .addInfraServices(saltBootstrapServiceStatus)
                .addCmServices(cmAgentServiceStatus)
                .build();
        NodeStatus nodeStatus = NodeStatus.newBuilder()
                .setStatusDetails(statusDetails)
                .setServicesDetails(servicesDetails)
                .build();
        NodeStatusReport nodeStatusReport = NodeStatusReport.newBuilder()
                .addNodes(nodeStatus)
                .build();
        RPCResponse<NodeStatusReport> response = new RPCResponse<>();
        response.setResult(nodeStatusReport);
        return response;
    }

}