package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.GATEWAY_SERVICES_STATUS_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SERVICES_CHECK_FAILED;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.SERVICES_CHECK_FAILED_DETAILS;
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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServiceStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServicesStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class NodeServicesCheckerConclusionStepTest {

    @Mock
    private StackService stackService;

    @Mock
    private CdpDoctorService cdpDoctorService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private NodeServicesCheckerConclusionStep underTest;

    @BeforeEach
    public void setup() {
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(new Stack());
    }

    @Test
    public void checkShouldBeSuccessfulIfNodeStatusReportFailsForOlderImageVersions() throws CloudbreakOrchestratorFailedException {
        when(cdpDoctorService.getServicesStatusForMinions(any())).thenThrow(new CloudbreakServiceException("error"));
        when(cloudbreakMessagesService.getMessage(GATEWAY_SERVICES_STATUS_FAILED)).thenReturn("gw service check failed");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("gw service check failed", stepResult.getConclusion());
        assertEquals("error", stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(cdpDoctorService, times(1)).getServicesStatusForMinions(any());
    }

    @Test
    public void checkShouldBeSuccessfulIfNoNodesWithUnhealthyServicesFound() throws CloudbreakOrchestratorFailedException {
        when(cdpDoctorService.getServicesStatusForMinions(any())).thenReturn(Map.of("host1", new CdpDoctorServicesStatusResponse()));
        Conclusion stepResult = underTest.check(1L);

        assertFalse(stepResult.isFailureFound());
        assertNull(stepResult.getConclusion());
        assertNull(stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(cdpDoctorService, times(1)).getServicesStatusForMinions(any());
    }

    @Test
    public void checkShouldFailAndReturnConclusionIfNodeWithUnhealthyServicesFound() throws CloudbreakOrchestratorFailedException {
        CdpDoctorServicesStatusResponse servicesStatusResponse = new CdpDoctorServicesStatusResponse();
        CdpDoctorServiceStatus cmService = new CdpDoctorServiceStatus();
        cmService.setName("cmService");
        cmService.setStatus(CdpDoctorCheckStatus.NOK);
        servicesStatusResponse.setCmServices(List.of(cmService));
        CdpDoctorServiceStatus infraService = new CdpDoctorServiceStatus();
        infraService.setName("infraService");
        infraService.setStatus(CdpDoctorCheckStatus.NOK);
        servicesStatusResponse.setInfraServices(List.of(infraService));
        when(cdpDoctorService.getServicesStatusForMinions(any())).thenReturn(Map.of("host1", servicesStatusResponse));
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SERVICES_CHECK_FAILED), any())).thenReturn("unhealthy nodes");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(SERVICES_CHECK_FAILED_DETAILS), any())).thenReturn("unhealthy nodes details");
        Conclusion stepResult = underTest.check(1L);

        assertTrue(stepResult.isFailureFound());
        assertEquals("unhealthy nodes", stepResult.getConclusion());
        assertEquals("unhealthy nodes details", stepResult.getDetails());
        assertEquals(NodeServicesCheckerConclusionStep.class, stepResult.getConclusionStepClass());
        verify(cdpDoctorService, times(1)).getServicesStatusForMinions(any());
    }

}