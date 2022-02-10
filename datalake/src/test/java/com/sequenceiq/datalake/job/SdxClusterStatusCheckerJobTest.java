package com.sequenceiq.datalake.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.job.SdxClusterStatusCheckerJobTest.TestAppContext;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowLogService;

import io.opentracing.Tracer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestAppContext.class)
class SdxClusterStatusCheckerJobTest {

    private static final Long SDX_ID = 456L;

    private static final Long STACK_ID = 123L;

    @Inject
    private SdxClusterStatusCheckerJob underTest;

    @Inject
    private StatusCheckerJobService jobService;

    @MockBean
    private SdxClusterRepository sdxClusterRepository;

    @MockBean
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @MockBean
    private SdxStatusService sdxStatusService;

    @MockBean
    private Tracer tracer;

    @MockBean
    private FlowLogService flowLogService;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private AutoscaleV4Endpoint autoscaleV4Endpoint;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private SdxCluster sdxCluster;

    private StackStatusV4Response stack;

    private SdxStatusEntity status;

    private JobDataMap jobDataMap;

    @BeforeEach
    void setUp() {
        underTest.setLocalId(SDX_ID.toString());
        underTest.setRemoteResourceCrn(STACK_ID.toString());

        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("data-lake-cluster");
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.of(sdxCluster));

        stack = new StackStatusV4Response();
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
        when(autoscaleV4Endpoint.getStatusByCrn(STACK_ID.toString())).thenReturn(stack);

        status = new SdxStatusEntity();
        status.setDatalake(sdxCluster);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);

        jobDataMap = new JobDataMap();
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        when(flowLogService.isOtherFlowRunning(any())).thenReturn(false);
    }

    @Test
    public void doNotExecuteTracedJobWhenThereIsARunningFlow() {
        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        when(flowLogService.isOtherFlowRunning(any())).thenReturn(true);
        underTest.executeTracedJob(context);
        verify(cloudbreakInternalCrnClient, times(0)).withInternalCrn();
    }

    @Test
    void available() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setStatus(Status.AVAILABLE);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(), any(), anyString(), eq(sdxCluster));
    }

    @Test
    void ambiguousToClusterUnreachable() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.CLUSTER_AMBIGUOUS);
        stack.setStatus(Status.UNREACHABLE);
        stack.setStatusReason("connection failure");

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.CLUSTER_UNREACHABLE),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                any(),
                eq("connection failure"),
                eq(sdxCluster));
    }

    @Test
    void ambiguousToNodeFailure() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.CLUSTER_AMBIGUOUS);
        stack.setStatus(Status.NODE_FAILURE);
        stack.setStatusReason("cm agent down");

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.NODE_FAILURE),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                any(),
                eq("cm agent down"),
                eq(sdxCluster));
    }

    @Test
    void runningToDeletedOnProviderSide() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setStatus(Status.DELETED_ON_PROVIDER_SIDE);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE),
                eq(ResourceEvent.SDX_CLUSTER_DELETED_ON_PROVIDER_SIDE),
                eq(""),
                eq(sdxCluster));
        verify(jobService, times(1)).scheduleLongIntervalCheck(any());
    }

    @Test
    void deletedOnProviderSideToRunning() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
        stack.setStatus(Status.AVAILABLE);
        stack.setClusterStatus(Status.AVAILABLE);
        jobDataMap.put(StatusCheckerJobService.SYNC_JOB_TYPE, StatusCheckerJobService.LONG_SYNC_JOB_TYPE);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.RUNNING),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                eq(Collections.singleton("data-lake-cluster")),
                eq(""),
                eq(sdxCluster));
        verify(jobService, times(1)).schedule(any());
    }

    @Test
    void longSyncJobShouldntChangeScheduleWhenDeletedOnProviderSide() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
        stack.setStatus(Status.DELETED_ON_PROVIDER_SIDE);
        jobDataMap.put(StatusCheckerJobService.SYNC_JOB_TYPE, StatusCheckerJobService.LONG_SYNC_JOB_TYPE);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(any(), any(), any(), any(), any());
        verify(jobService, times(0)).schedule(any());
        verify(jobService, times(0)).scheduleLongIntervalCheck(any());
    }

    @Test
    void deletedOnProviderSideToDeleted() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
        stack.setStatus(Status.DELETE_COMPLETED);
        jobDataMap.put(StatusCheckerJobService.SYNC_JOB_TYPE, StatusCheckerJobService.LONG_SYNC_JOB_TYPE);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.STACK_DELETED),
                eq(ResourceEvent.SDX_CLUSTER_DELETION_FINISHED),
                eq(""),
                eq(sdxCluster));
        verify(jobService, times(1)).unschedule(any());
    }

    @Test
    void stoppedToRunning() throws JobExecutionException {
        setUpSdxStatus(DatalakeStatusEnum.STOPPED);
        stack.setStatus(Status.AVAILABLE);
        stack.setClusterStatus(Status.AVAILABLE);

        underTest.executeTracedJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.RUNNING),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                eq(Collections.singleton("data-lake-cluster")),
                eq(""),
                eq(sdxCluster));
    }

    private void setUpSdxStatus(DatalakeStatusEnum status) {
        this.status.setStatus(status);
    }

    @Configuration
    @Import(SdxClusterStatusCheckerJob.class)
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private StatusCheckerJobService jobService;

    }
}
