package com.sequenceiq.datalake.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
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
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.job.SdxClusterStatusCheckerJobTest.TestAppContext;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestAppContext.class)
class SdxClusterStatusCheckerJobTest {

    private static final Long SDX_ID = 456L;

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:accountId:cluster:resourceId";

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
        underTest.setRemoteResourceCrn(RESOURCE_CRN.toString());

        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("data-lake-cluster");
        sdxCluster.setId(SDX_ID);
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.of(sdxCluster));

        stack = new StackStatusV4Response();
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
        when(autoscaleV4Endpoint.getStatusByCrn(RESOURCE_CRN.toString())).thenReturn(stack);

        status = new SdxStatusEntity();
        status.setDatalake(sdxCluster);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);

        jobDataMap = new JobDataMap();
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        when(flowLogService.isOtherFlowRunning(any())).thenReturn(false);
    }

    @Test
    public void doNotExecuteJobWhenThereIsARunningFlow() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(flowLogService.isOtherFlowRunning(any())).thenReturn(true);
        underTest.executeJob(context);
        verify(cloudbreakInternalCrnClient, times(0)).withInternalCrn();
    }

    @Test
    void available() {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setStatus(Status.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(), any(ResourceEvent.class), anyString(), eq(sdxCluster));
    }

    @Test
    void ambiguousToClusterUnreachable() {
        setUpSdxStatus(DatalakeStatusEnum.CLUSTER_AMBIGUOUS);
        stack.setStatus(Status.UNREACHABLE);
        stack.setStatusReason("connection failure");

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.CLUSTER_UNREACHABLE),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                any(),
                eq("connection failure"),
                eq(sdxCluster));
    }

    @Test
    void ambiguousToNodeFailure() {
        setUpSdxStatus(DatalakeStatusEnum.CLUSTER_AMBIGUOUS);
        stack.setStatus(Status.NODE_FAILURE);
        stack.setStatusReason("cm agent down");

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.NODE_FAILURE),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                any(),
                eq("cm agent down"),
                eq(sdxCluster));
    }

    @Test
    void runningToDeletedOnProviderSide() {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setStatus(Status.DELETED_ON_PROVIDER_SIDE);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE),
                eq(ResourceEvent.SDX_CLUSTER_DELETED_ON_PROVIDER_SIDE),
                eq(""),
                eq(sdxCluster));
        verify(jobService, times(1)).scheduleLongIntervalCheck(any(), any());
    }

    @Test
    void deletedOnProviderSideToRunning() {
        setUpSdxStatus(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
        stack.setStatus(Status.AVAILABLE);
        stack.setClusterStatus(Status.AVAILABLE);
        jobDataMap.put(StatusCheckerJobService.SYNC_JOB_TYPE, StatusCheckerJobService.LONG_SYNC_JOB_TYPE);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.RUNNING),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                eq(Collections.singleton("data-lake-cluster")),
                eq(""),
                eq(sdxCluster));
        verify(jobService, times(1)).schedule(any(), any());
    }

    @Test
    void longSyncJobShouldntChangeScheduleWhenDeletedOnProviderSide() {
        setUpSdxStatus(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
        stack.setStatus(Status.DELETED_ON_PROVIDER_SIDE);
        jobDataMap.put(StatusCheckerJobService.SYNC_JOB_TYPE, StatusCheckerJobService.LONG_SYNC_JOB_TYPE);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(any(), any(), any(), any(), eq(sdxCluster));
        verify(jobService, times(0)).schedule(any());
        verify(jobService, times(0)).scheduleLongIntervalCheck(any());
    }

    @Test
    void deletedOnProviderSideToDeleted() {
        setUpSdxStatus(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
        stack.setStatus(Status.DELETE_COMPLETED);
        jobDataMap.put(StatusCheckerJobService.SYNC_JOB_TYPE, StatusCheckerJobService.LONG_SYNC_JOB_TYPE);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.STACK_DELETED),
                eq(ResourceEvent.SDX_CLUSTER_DELETION_FINISHED),
                eq(""),
                eq(sdxCluster));
        verify(jobService, times(1)).unschedule(any());
    }

    @Test
    void stoppedToRunning() {
        setUpSdxStatus(DatalakeStatusEnum.STOPPED);
        stack.setStatus(Status.AVAILABLE);
        stack.setClusterStatus(Status.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.RUNNING),
                eq(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
                eq(Collections.singleton("data-lake-cluster")),
                eq(""),
                eq(sdxCluster));
    }

    @Test
    void unscheduleSdxClustersInStackDeletedStatus() {
        setUpSdxStatus(DatalakeStatusEnum.STACK_DELETED);

        underTest.executeJob(jobExecutionContext);

        verify(sdxStatusService, times(1)).getActualStatusForSdx(eq(sdxCluster));
        verify(jobService, times(1)).unschedule(eq(String.valueOf(SDX_ID)));
        verify(autoscaleV4Endpoint, never()).getStatusByCrn(anyString());
    }

    @ParameterizedTest
    @EnumSource(ProviderSyncState.class)
    void testUpdateProviderSyncStateIfDifferent(ProviderSyncState newState) {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setProviderSyncStates(Set.of(newState));
        stack.setStatus(Status.AVAILABLE);
        sdxCluster.setProviderSyncStates(Set.of(ProviderSyncState.VALID));

        underTest.executeJob(jobExecutionContext);
        if (newState == ProviderSyncState.VALID) {
            verify(sdxClusterRepository, never()).updateProviderSyncStates(sdxCluster.getId(), stack.getProviderSyncStates());
        } else {
            verify(sdxClusterRepository, times(1)).updateProviderSyncStates(sdxCluster.getId(), stack.getProviderSyncStates());
        }
    }

    @ParameterizedTest
    @EnumSource(CertExpirationState.class)
    void testUpdateCertExpirationStateIfDifferent(CertExpirationState newState) {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setCertExpirationState(newState);
        stack.setStatus(Status.AVAILABLE);
        sdxCluster.setCertExpirationState(CertExpirationState.VALID);

        underTest.executeJob(jobExecutionContext);

        if (newState == CertExpirationState.VALID) {
            verify(sdxClusterRepository, never()).updateCertExpirationState(sdxCluster.getId(),
                    stack.getCertExpirationState(),
                    stack.getCertExpirationDetails());
        } else {
            verify(sdxClusterRepository, times(1)).updateCertExpirationState(sdxCluster.getId(),
                    stack.getCertExpirationState(),
                    stack.getCertExpirationDetails());
        }
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
