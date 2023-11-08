package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.GATEWAY_NETWORK_STATUS_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CCM_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CCM_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NEIGHBOUR_NOT_ACCESSIBLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.NETWORK_NGINX_UNREACHABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class NetworkCheckerConclusionStepTest {

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private StackService stackService;

    @Mock
    private CdpDoctorService cdpDoctorService;

    @InjectMocks
    private NetworkCheckerConclusionStep underTest;

    @BeforeEach
    public void setup() throws CloudbreakOrchestratorFailedException {
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(new Stack());
    }

    @Test
    public void checkShouldBeSuccessfulIfNetworkReportFailedForOlderImageVersions() throws CloudbreakOrchestratorFailedException {
        when(cdpDoctorService.getNetworkStatusForMinions(any())).thenThrow(new CloudbreakOrchestratorFailedException("error"));
        when(cloudbreakMessagesService.getMessage(eq(GATEWAY_NETWORK_STATUS_FAILED))).thenReturn("gateway network status check failed");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("gateway network status check failed", stepResult.getConclusion());
        assertEquals("error", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }

    @Test
    public void checkShouldBeSuccessfulIfNetworkIsHealthy() throws CloudbreakOrchestratorFailedException {
        when(cdpDoctorService.getNetworkStatusForMinions(any())).thenReturn(Map.of("host1", new CdpDoctorNetworkStatusResponse()));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfCcmIsNotAccessible() throws CloudbreakOrchestratorFailedException {
        CdpDoctorNetworkStatusResponse networkStatusResponse = new CdpDoctorNetworkStatusResponse();
        networkStatusResponse.setCcmEnabled(true);
        networkStatusResponse.setCcmAccessible(CdpDoctorCheckStatus.NOK);
        when(cdpDoctorService.getNetworkStatusForMinions(any())).thenReturn(Map.of("host1", networkStatusResponse));
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CCM_NOT_ACCESSIBLE), any())).thenReturn("ccm error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CCM_NOT_ACCESSIBLE_DETAILS), any(), any()))
                .thenReturn("ccm error details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[ccm error]", stepResult.getConclusion());
        assertEquals("[ccm error details]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfClouderaComIsNotAccessible() throws CloudbreakOrchestratorFailedException {
        CdpDoctorNetworkStatusResponse networkStatusResponse = new CdpDoctorNetworkStatusResponse();
        networkStatusResponse.setClouderaComAccessible(CdpDoctorCheckStatus.NOK);
        when(cdpDoctorService.getNetworkStatusForMinions(any())).thenReturn(Map.of("host1", networkStatusResponse));
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE), any())).thenReturn("cloudera.com error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS), any(), any()))
                .thenReturn("cloudera.com error details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[cloudera.com error]", stepResult.getConclusion());
        assertEquals("[cloudera.com error details]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNeighboursAreNotAccessible() throws CloudbreakOrchestratorFailedException {
        CdpDoctorNetworkStatusResponse networkStatusResponse = new CdpDoctorNetworkStatusResponse();
        networkStatusResponse.setNeighbourScan(true);
        networkStatusResponse.setAnyNeighboursAccessible(CdpDoctorCheckStatus.NOK);
        when(cdpDoctorService.getNetworkStatusForMinions(any())).thenReturn(Map.of("host1", networkStatusResponse));
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_NEIGHBOUR_NOT_ACCESSIBLE), any())).thenReturn("neighbour error");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS), any(), any()))
                .thenReturn("neighbour error details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("[neighbour error]", stepResult.getConclusion());
        assertEquals("[neighbour error details]", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNginxIsNotReachable() throws CloudbreakOrchestratorFailedException {
        when(cdpDoctorService.getNetworkStatusForMinions(any())).thenThrow(new CloudbreakOrchestratorFailedException("nginx is unreachable details"));
        when(cloudbreakMessagesService.getMessage(eq(NETWORK_NGINX_UNREACHABLE))).thenReturn("nginx is unreachable");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("nginx is unreachable", stepResult.getConclusion());
        assertEquals("nginx is unreachable details", stepResult.getDetails());
        assertEquals(NetworkCheckerConclusionStep.class, stepResult.getConclusionStepClass());
    }
}
