package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
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
    private NodeStatusService nodeStatusService;

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
        given(stackService.getById(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.getEnvironmentCrn()).willReturn(ENVIRONMENT_CRN);
        given(nodeStatusService.getMeteringReport(anyLong())).willReturn(createRpcResponse(false, false));
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(nodeStatusService, times(1)).getMeteringReport(anyLong());
        verify(usageReporter, times(0)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportWithDbusUnreachable() {
        // GIVEN
        given(stackService.getById(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.getEnvironmentCrn()).willReturn(ENVIRONMENT_CRN);
        given(nodeStatusService.getMeteringReport(anyLong())).willReturn(createRpcResponse(true, false));
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(nodeStatusService, times(1)).getMeteringReport(anyLong());
        verify(usageReporter, times(1)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportWithConfigError() {
        // GIVEN
        given(stackService.getById(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.getEnvironmentCrn()).willReturn(ENVIRONMENT_CRN);
        given(nodeStatusService.getMeteringReport(anyLong())).willReturn(createRpcResponse(false, true));
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(nodeStatusService, times(1)).getMeteringReport(anyLong());
        verify(usageReporter, times(1)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportNoResponse() {
        // GIVEN
        given(stackService.getById(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(true);
        given(nodeStatusService.getMeteringReport(anyLong())).willReturn(null);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(nodeStatusService, times(1)).getMeteringReport(anyLong());
        verify(usageReporter, times(0)).cdpDiagnosticsEvent(any());
    }

    @Test
    public void testNodeStatusMeteringReportForDatalake() {
        // GIVEN
        given(stackService.getById(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(true);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(nodeStatusService, times(0)).getMeteringReport(anyLong());
    }

    @Test
    public void testNodeStatusMeteringReportWithMeteringDisabled() {
        // GIVEN
        given(stackService.getById(anyLong())).willReturn(stack);
        given(stack.isDatalake()).willReturn(false);
        given(meteringConfiguration.isEnabled()).willReturn(false);
        // WHEN
        underTest.nodeStatusMeteringReport(STACK_ID);
        // THEN
        verify(nodeStatusService, times(0)).getMeteringReport(anyLong());
    }

    private RPCResponse<NodeStatusProto.NodeStatusReport> createRpcResponse(boolean databusUnreachable, boolean configError) {
        RPCResponse<NodeStatusProto.NodeStatusReport> rpcResponse = new RPCResponse<>();
        NodeStatusProto.NodeStatusReport nodeStatusReport = NodeStatusProto.NodeStatusReport.newBuilder()
                .addNodes(NodeStatusProto.NodeStatus.newBuilder()
                        .setStatusDetails(NodeStatusProto.StatusDetails.newBuilder()
                                .setHost("host1"))
                        .setMeteringDetails(NodeStatusProto.MeteringDetails
                                .newBuilder()
                                .setDatabusReachable(databusUnreachable ? NodeStatusProto.HealthStatus.NOK : NodeStatusProto.HealthStatus.OK)
                                .setHeartbeatConfig(configError ? NodeStatusProto.HealthStatus.NOK : NodeStatusProto.HealthStatus.OK))
                        .build())
                .build();
        rpcResponse.setResult(nodeStatusReport);
        return rpcResponse;
    }

}
