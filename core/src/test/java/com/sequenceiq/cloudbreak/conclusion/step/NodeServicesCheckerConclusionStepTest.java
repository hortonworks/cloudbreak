package com.sequenceiq.cloudbreak.conclusion.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@ExtendWith(MockitoExtension.class)
class NodeServicesCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @InjectMocks
    private NodeServicesCheckerConclusionStep underTest;

    @Test
    public void checkShouldBeSuccessfulIfNodeStatusReportFailsForOlderImageVersions() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenThrow(new CloudbreakServiceException("error"));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
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
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(createNodeStatusResponse(HealthStatus.OK));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNodeWithUnhealthyServicesFound() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(createNodeStatusResponse(HealthStatus.NOK));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("There are unhealthy services on nodes: [host1]. " +
                "Please check the instances on your cloud provider for further details.", stepResult.getConclusion());
        assertEquals("There are unhealthy services on nodes: {host1=[salt-bootstrap]}", stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    private RPCResponse<NodeStatusReport> createNodeStatusResponse(HealthStatus saltBootstrapServiceStatus) {
        StatusDetails statusDetails = StatusDetails.newBuilder()
                .setHost("host1")
                .build();
        ServiceStatus serviceStatus = ServiceStatus.newBuilder()
                .setName("salt-bootstrap")
                .setStatus(saltBootstrapServiceStatus)
                .build();
        ServicesDetails  servicesDetails = ServicesDetails.newBuilder()
                .addInfraServices(serviceStatus)
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