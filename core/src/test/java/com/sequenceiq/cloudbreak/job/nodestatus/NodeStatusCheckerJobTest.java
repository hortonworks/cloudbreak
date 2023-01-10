package com.sequenceiq.cloudbreak.job.nodestatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.node.health.client.model.CdpNodeStatusRequest;
import com.sequenceiq.node.health.client.model.CdpNodeStatuses;

@ExtendWith(MockitoExtension.class)
public class NodeStatusCheckerJobTest {

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private NodeStatusService nodeStatusService;

    @Mock
    private StackService stackService;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private NodeStatusCheckerJobService jobService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @InjectMocks
    private NodeStatusCheckerJob underTest;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private Telemetry telemetry;

    @Captor
    private ArgumentCaptor<CdpNodeStatusRequest> captor;

    @BeforeEach
    public void setUp() {
        underTest = new NodeStatusCheckerJob();
        MockitoAnnotations.openMocks(this);
        underTest.setLocalId("1");
        underTest.setRemoteResourceCrn(DATAHUB_CRN);
    }

    @Test
    public void testNodeStatusCheckOnDataHubWhenEntitlementIsEnabled() throws JobExecutionException {
        Stack stack = stack(DetailedStackStatus.AVAILABLE, StackType.WORKLOAD);
        when(stackService.getByIdWithGatewayInTransaction(anyLong())).thenReturn(stack);
        when(entitlementService.datahubNodestatusCheckEnabled(anyString())).thenReturn(true);
        when(nodeStatusService.getNodeStatuses(any(), any())).thenReturn(CdpNodeStatuses.Builder
                .builder()
                .build());

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService).getByIdWithGatewayInTransaction(1L);

        verify(nodeStatusService).getNodeStatuses(eq(stack), captor.capture());
        CdpNodeStatusRequest nodeStatusRequest = captor.getValue();
        assertTrue(nodeStatusRequest.isMetering());
        assertTrue(nodeStatusRequest.isCmMonitoring());
        assertTrue(nodeStatusRequest.isSkipObjectMapping());
        assertFalse(nodeStatusRequest.isNetworkOnly());
    }

    @Test
    public void testNodeStatusCheckOnDataHubWhenEntitlementIsNotEnabled() throws JobExecutionException {
        Stack stack = stack(DetailedStackStatus.AVAILABLE, StackType.WORKLOAD);
        when(stackService.getByIdWithGatewayInTransaction(anyLong())).thenReturn(stack);
        when(entitlementService.datahubNodestatusCheckEnabled(anyString())).thenReturn(false);

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService).getByIdWithGatewayInTransaction(1L);
        verify(nodeStatusService, never()).getNodeStatuses(any(), any());
    }

    @Test
    public void testNodeStatusCheckChecksOnlyNetworkWhenNewMonitoringIsConfigured() throws JobExecutionException {
        Stack stack = stack(DetailedStackStatus.AVAILABLE, StackType.DATALAKE);
        when(stackService.getByIdWithGatewayInTransaction(anyLong())).thenReturn(stack);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(true);
        when(componentConfigProviderService.getTelemetry(anyLong())).thenReturn(telemetry);
        when(nodeStatusService.getNodeStatuses(any(), any())).thenReturn(CdpNodeStatuses.Builder
                .builder()
                .build());

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService).getByIdWithGatewayInTransaction(1L);

        verify(nodeStatusService).getNodeStatuses(eq(stack), captor.capture());
        CdpNodeStatusRequest nodeStatusRequest = captor.getValue();
        assertFalse(nodeStatusRequest.isMetering());
        assertFalse(nodeStatusRequest.isCmMonitoring());
        assertFalse(nodeStatusRequest.isSkipObjectMapping());
        assertTrue(nodeStatusRequest.isNetworkOnly());
    }

    @Test
    public void testNodeStatusCheckDoesNotCheckMeteringOnDataLake() throws JobExecutionException {
        Stack stack = stack(DetailedStackStatus.AVAILABLE, StackType.DATALAKE);
        when(stackService.getByIdWithGatewayInTransaction(anyLong())).thenReturn(stack);
        when(nodeStatusService.getNodeStatuses(any(), any())).thenReturn(CdpNodeStatuses.Builder
                .builder()
                .build());

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService).getByIdWithGatewayInTransaction(1L);

        verify(nodeStatusService).getNodeStatuses(eq(stack), captor.capture());
        CdpNodeStatusRequest nodeStatusRequest = captor.getValue();
        assertFalse(nodeStatusRequest.isMetering());
        assertTrue(nodeStatusRequest.isCmMonitoring());
        assertTrue(nodeStatusRequest.isSkipObjectMapping());
        assertFalse(nodeStatusRequest.isNetworkOnly());
    }

    @Test
    public void testNodeStatusCheckIsUnscheduled() throws JobExecutionException {
        Stack stack = stack(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE, StackType.DATALAKE);
        when(stackService.getByIdWithGatewayInTransaction(anyLong())).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService).getByIdWithGatewayInTransaction(1L);
        verify(nodeStatusService, never()).getNodeStatuses(any(), any());
        verify(jobService).unschedule("1");
    }

    @Test
    public void testNodeStatusCheckIsSkipped() throws JobExecutionException {
        Stack stack = stack(DetailedStackStatus.CLUSTER_RECOVERY_IN_PROGRESS, StackType.DATALAKE);
        when(stackService.getByIdWithGatewayInTransaction(anyLong())).thenReturn(stack);

        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService).getByIdWithGatewayInTransaction(1L);
        verify(nodeStatusService, never()).getNodeStatuses(any(), any());
        verify(jobService, never()).unschedule(anyString());
    }

    private Stack stack(DetailedStackStatus status, StackType stackType) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setStackStatus(new StackStatus(stack, status));
        stack.setResourceCrn(DATAHUB_CRN);
        stack.setType(stackType);
        return stack;
    }
}