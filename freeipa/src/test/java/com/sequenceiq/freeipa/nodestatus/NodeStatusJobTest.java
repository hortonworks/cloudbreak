package com.sequenceiq.freeipa.nodestatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.FreeIpaNodeStatusService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.node.health.client.model.CdpNodeStatusRequest;
import com.sequenceiq.node.health.client.model.CdpNodeStatuses;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class NodeStatusJobTest {

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaNodeStatusService freeIpaNodeStatusService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private NodeStatusJobService nodeStatusJobService;

    @Mock
    private Tracer tracer;

    @InjectMocks
    private NodeStatusJob underTest;

    @Mock
    private JobExecutionContext context;

    @Mock
    private Stack stack;

    @Mock
    private Telemetry telemetry;

    @Captor
    private ArgumentCaptor<CdpNodeStatusRequest> captor;

    @BeforeEach
    public void setUp() {
        underTest = new NodeStatusJob(tracer);
        MockitoAnnotations.openMocks(this);
        underTest.setLocalId("1");
        underTest.setRemoteResourceCrn("crn");
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
    }

    @Test
    public void testNodeStatusCheckWhenCommonMonitoringIsEnabled() throws JobExecutionException, FreeIpaClientException {
        when(stack.getStackStatus()).thenReturn(new StackStatus(stack, "Status reason", DetailedStackStatus.AVAILABLE));
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(instanceMetaData));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(true);
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(freeIpaNodeStatusService.nodeStatusReport(any(), any(), any())).thenReturn(CdpNodeStatuses.Builder.builder().build());

        underTest.executeTracedJob(context);

        verify(freeIpaNodeStatusService).nodeStatusReport(eq(stack), eq(instanceMetaData), captor.capture());
        CdpNodeStatusRequest statusRequest = captor.getValue();
        assertTrue(statusRequest.isNetworkOnly());
        assertTrue(statusRequest.isSkipObjectMapping());
        assertFalse(statusRequest.isCmMonitoring());
        assertFalse(statusRequest.isMetering());
    }

    @Test
    public void testNodeStatusCheckWhenCommonMonitoringIsNotEnabled() throws JobExecutionException, FreeIpaClientException {
        when(stack.getStackStatus()).thenReturn(new StackStatus(stack, "Status reason", DetailedStackStatus.AVAILABLE));
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(instanceMetaData));
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(false);
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(freeIpaNodeStatusService.nodeStatusReport(any(), any(), any())).thenReturn(CdpNodeStatuses.Builder.builder().build());

        underTest.executeTracedJob(context);

        verify(freeIpaNodeStatusService).nodeStatusReport(eq(stack), eq(instanceMetaData), captor.capture());
        CdpNodeStatusRequest statusRequest = captor.getValue();
        assertFalse(statusRequest.isNetworkOnly());
        assertTrue(statusRequest.isSkipObjectMapping());
        assertFalse(statusRequest.isCmMonitoring());
        assertFalse(statusRequest.isMetering());
    }

    @Test
    public void testNodeStatusCheckUnschedule() throws JobExecutionException, FreeIpaClientException {
        when(stack.getStackStatus()).thenReturn(new StackStatus(stack, "Status reason", DetailedStackStatus.DELETED_ON_PROVIDER_SIDE));
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);

        underTest.executeTracedJob(context);

        verify(nodeStatusJobService).unschedule(stack);
        verify(freeIpaNodeStatusService, never()).nodeStatusReport(any(), any(), any());
    }

    @Test
    public void testNodeStatusCheckIsSkippedWhenFlowIsRunning() throws JobExecutionException, FreeIpaClientException {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(true);

        underTest.executeTracedJob(context);

        verify(nodeStatusJobService, never()).unschedule(stack);
        verify(freeIpaNodeStatusService, never()).nodeStatusReport(any(), any(), any());
    }
}