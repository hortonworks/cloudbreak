package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CCM_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CCM_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NEIGHBOUR_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NGINX_UNREACHABLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NODE_STATUS_MONITOR_UNREACHABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;

@ExtendWith(MockitoExtension.class)
class NetworkCheckerConclusionStepTest {

    @Mock
    private NodeStatusService nodeStatusService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private NetworkCheckerConclusionStep underTest;

    @Test
    public void checkShouldBeSuccessfulIfNetworkReportFailedForOlderImageVersions() {
        when(cloudbreakMessagesService.getMessage(eq(NODE_STATUS_MONITOR_UNREACHABLE))).thenReturn("node status unreachable");
        when(nodeStatusService.getNetworkReport(eq(1L))).thenThrow(new CloudbreakServiceException("error"));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("node status unreachable", stepResult.getConclusion());
        assertEquals("error", stepResult.getDetails());
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
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CCM_NOT_ACCESSIBLE), any())).thenReturn("ccm error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CCM_NOT_ACCESSIBLE_DETAILS), any(), any()))
                .thenReturn("ccm error details");
        NetworkDetails networkDetails = NetworkDetails.newBuilder()
                .setCcmEnabled(true)
                .setCcmAccessible(HealthStatus.NOK)
                .setClouderaComAccessible(HealthStatus.OK)
                .setAnyNeighboursAccessible(HealthStatus.OK)
                .build();
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(createNetworkReportResponse(networkDetails));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[ccm error]", stepResult.getConclusion());
        assertEquals("[ccm error details]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfClouderaComIsNotAccessible() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE), any())).thenReturn("cloudera.com error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS), any(), any()))
                .thenReturn("cloudera.com error details");
        NetworkDetails networkDetails = NetworkDetails.newBuilder()
                .setCcmEnabled(true)
                .setCcmAccessible(HealthStatus.OK)
                .setClouderaComAccessible(HealthStatus.NOK)
                .setAnyNeighboursAccessible(HealthStatus.OK)
                .build();
        when(nodeStatusService.getNetworkReport(eq(1L))).thenReturn(createNetworkReportResponse(networkDetails));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[cloudera.com error]", stepResult.getConclusion());
        assertEquals("[cloudera.com error details]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNeighboursAreNotAccessible() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_NEIGHBOUR_NOT_ACCESSIBLE), any())).thenReturn("neighbour error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS), any(), any()))
                .thenReturn("neighbour error details");
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
        assertEquals("[neighbour error]", stepResult.getConclusion());
        assertEquals("[neighbour error details]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(nodeStatusService, times(1)).getNetworkReport(eq(1L));
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNginxIsNotReachable() {
        when(cloudbreakMessagesService.getMessage(eq(NETWORK_NGINX_UNREACHABLE))).thenReturn("nginx is unreachable");
        when(nodeStatusService.getNetworkReport(eq(1L)))
                .thenThrow(new CloudbreakServiceException("nginx is unreachable details"));
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("nginx is unreachable", stepResult.getConclusion());
        assertEquals("nginx is unreachable details", stepResult.getDetails());
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
