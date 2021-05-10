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

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@ExtendWith(MockitoExtension.class)
class SaltBootstrapCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @InjectMocks
    private SaltBootstrapCheckerConclusionStep underTest;

    @Test
    public void checkShouldBeSuccessfulIfNodeStatusReportIsNullForOlderImageVersions() {
        RPCResponse<NodeStatusProto.NodeStatusReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(response);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltBootstrapCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    @Test
    public void checkShouldBeSuccessfulIfNoUnreachableNodeFound() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(createNodeStatusResponse(NodeStatusProto.HealthStatus.OK));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(SaltBootstrapCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfUnreachableNodeFound() {
        when(nodeStatusService.getServicesReport(eq(1L))).thenReturn(createNodeStatusResponse(NodeStatusProto.HealthStatus.NOK));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("Unreachable nodes: [host1]. Please check the instances on your cloud provider for further details.", stepResult.getConclusion());
        assertEquals("Unreachable salt bootstrap nodes: [host1]", stepResult.getDetails());
        assertEquals(SaltBootstrapCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getServicesReport(eq(1L));
    }

    private RPCResponse<NodeStatusProto.NodeStatusReport> createNodeStatusResponse(NodeStatusProto.HealthStatus saltBootstrapServiceStatus) {
        NodeStatusProto.StatusDetails statusDetails = NodeStatusProto.StatusDetails.newBuilder()
                .setHost("host1")
                .build();
        NodeStatusProto.ServiceStatus serviceStatus = NodeStatusProto.ServiceStatus.newBuilder()
                .setName("salt-bootstrap")
                .setStatus(saltBootstrapServiceStatus)
                .build();
        NodeStatusProto.ServicesDetails  servicesDetails = NodeStatusProto.ServicesDetails.newBuilder()
                .addInfraServices(serviceStatus)
                .build();
        NodeStatusProto.NodeStatus nodeStatus = NodeStatusProto.NodeStatus.newBuilder()
                .setStatusDetails(statusDetails)
                .setServicesDetails(servicesDetails)
                .build();
        NodeStatusProto.NodeStatusReport nodeStatusReport = NodeStatusProto.NodeStatusReport.newBuilder()
                .addNodes(nodeStatus)
                .build();
        RPCResponse<NodeStatusProto.NodeStatusReport> response = new RPCResponse<>();
        response.setResult(nodeStatusReport);
        return response;
    }

}