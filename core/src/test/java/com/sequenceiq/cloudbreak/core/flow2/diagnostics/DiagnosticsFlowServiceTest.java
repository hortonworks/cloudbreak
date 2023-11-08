package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorMeteringStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsFlowServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "crn:cdp:environment:eu-1:1234:user:91011";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    @InjectMocks
    private DiagnosticsFlowService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Mock
    private MeteringConfiguration meteringConfiguration;

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private CdpDoctorService cdpDoctorService;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsFlowService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionEquals() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.8", "0.4.8");
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionLess() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.7", "0.4.8");
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionGreater() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.9", "0.4.8");
        // THEN
        assertTrue(result);
    }

    @Test
    public void testNodeStatusMeteringReportWithNoIssues() {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.getEnvironmentCrn()).willReturn(ENVIRONMENT_CRN);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(usageReporter, times(0)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportWithDbusUnreachable() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.getEnvironmentCrn()).willReturn(ENVIRONMENT_CRN);
        CdpDoctorMeteringStatusResponse meteringStatusResponse = new CdpDoctorMeteringStatusResponse();
        meteringStatusResponse.setDatabusReachable(CdpDoctorCheckStatus.NOK);
        given(cdpDoctorService.getMeteringStatusForMinions(any())).willReturn(Map.of("host1", meteringStatusResponse));
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(usageReporter, times(1)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testMeteringReportWithConfigError() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.getEnvironmentCrn()).willReturn(ENVIRONMENT_CRN);
        CdpDoctorMeteringStatusResponse meteringStatusResponse = new CdpDoctorMeteringStatusResponse();
        meteringStatusResponse.setHeartbeatConfig(CdpDoctorCheckStatus.NOK);
        given(cdpDoctorService.getMeteringStatusForMinions(any())).willReturn(Map.of("host1", meteringStatusResponse));
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(usageReporter, times(1)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportNoResponse() {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(usageReporter, times(0)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportForDatalake() {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(true);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(usageReporter, times(0)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportWithMeteringDisabled() {
        // GIVEN
        given(meteringConfiguration.isEnabled()).willReturn(false);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(usageReporter, times(0)).cdpDiagnosticsEvent(any());
    }

}
