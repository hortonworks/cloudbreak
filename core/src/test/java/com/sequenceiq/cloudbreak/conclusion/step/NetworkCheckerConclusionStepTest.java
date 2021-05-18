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
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NetworkDetails;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatus;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.NodeStatusReport;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto.StatusDetails;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@ExtendWith(MockitoExtension.class)
class NetworkCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @InjectMocks
    private NetworkCheckerConclusionStep underTest;

    @Test
    public void checkShouldBeSuccessfulIfNetworkReportFailedForOlderImageVersions() {
        when(nodeStatusService.getNetworkReport(eq(1L))).thenThrow(new CloudbreakServiceException("error"));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldBeSuccessfulIfNetworkReportIsNullForOlderImageVersions() {
        RPCResponse<NodeStatusReport> response = new RPCResponse<>();
        RPCMessage message = new RPCMessage();
        message.setMessage("rpc response");
        response.setMessages(List.of(message));
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(response);
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldBeSuccessfulIfNetworkIsHealthy() {
        NetworkDetails networkDetails = NetworkDetails.newBuilder()
                .setCcmEnabled(true)
                .setCcmAccessible(HealthStatus.OK)
                .setAnyNeighboursAccessible(HealthStatus.OK)
                .setClouderaComAccessible(HealthStatus.OK)
                .build();
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(createNetworkReportResponse(networkDetails));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfCcmIsNotAccessible() {
        NetworkDetails networkDetails = NetworkDetails.newBuilder()
                .setCcmEnabled(true)
                .setCcmAccessible(HealthStatus.NOK)
                .setClouderaComAccessible(HealthStatus.OK)
                .setAnyNeighboursAccessible(HealthStatus.OK)
                .build();
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(createNetworkReportResponse(networkDetails));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[CCM is not accessible from node host1. Please check network settings!]", stepResult.getConclusion());
        assertEquals("[CCM health status is NOK for node host1]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfClouderaComIsNotAccessible() {
        NetworkDetails networkDetails = NetworkDetails.newBuilder()
                .setCcmEnabled(true)
                .setCcmAccessible(HealthStatus.OK)
                .setClouderaComAccessible(HealthStatus.NOK)
                .setAnyNeighboursAccessible(HealthStatus.OK)
                .build();
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(createNetworkReportResponse(networkDetails));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[Cloudera.com is not accessible from node: host1. Please check network settings!]", stepResult.getConclusion());
        assertEquals("[Cloudera.com accessibility status is NOK for node host1]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNeighboursAreNotAccessible() {
        NetworkDetails networkDetails = NetworkDetails.newBuilder()
                .setCcmEnabled(true)
                .setNeighbourScan(true)
                .setCcmAccessible(HealthStatus.OK)
                .setClouderaComAccessible(HealthStatus.OK)
                .setAnyNeighboursAccessible(HealthStatus.NOK)
                .build();
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(createNetworkReportResponse(networkDetails));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[Node host1 cannot reach any neighbour nodes. Please check nodes and network settings!]", stepResult.getConclusion());
        assertEquals("[Neighbours accessibility status is NOK for node host1]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    private RPCResponse<NodeStatusReport> createNetworkReportResponse(NetworkDetails networkDetails) {
        StatusDetails statusDetails = StatusDetails.newBuilder()
                .setHost("host1")
                .build();
        NodeStatus nodeStatus = NodeStatus.newBuilder()
                .setStatusDetails(statusDetails)
                .setNetworkDetails(networkDetails)
                .build();
        NodeStatusReport nodeStatusReport = NodeStatusReport.newBuilder()
                .addNodes(nodeStatus)
                .build();
        RPCResponse<NodeStatusReport> response = new RPCResponse<>();
        response.setResult(nodeStatusReport);
        return response;
    }
}