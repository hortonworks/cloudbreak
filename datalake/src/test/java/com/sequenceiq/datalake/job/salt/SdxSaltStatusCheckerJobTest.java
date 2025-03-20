package com.sequenceiq.datalake.job.salt;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatusResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class SdxSaltStatusCheckerJobTest {

    private static final Long CLUSTER_ID = 1L;

    private static final String CRN = "crn";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private SdxSaltStatusCheckerJobService jobService;

    @Mock
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SdxSaltStatusCheckerJob underTest;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private SdxStatusEntity sdxStatus;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private JobKey jobKey;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SaltPasswordStatusResponse saltPasswordStatus;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest.setLocalId(String.valueOf(CLUSTER_ID));
        underTest.setRemoteResourceCrn(CRN);

        lenient().when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        lenient().when(flowLogService.isOtherFlowRunning(CLUSTER_ID)).thenReturn(false);
        lenient().when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatus);
        lenient().when(context.getJobDetail()).thenReturn(jobDetail);
        lenient().when(jobDetail.getKey()).thenReturn(jobKey);

        CloudbreakServiceCrnEndpoints endpoints = mock(CloudbreakServiceCrnEndpoints.class);
        lenient().when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(endpoints);
        lenient().when(endpoints.stackV4Endpoint()).thenReturn(stackV4Endpoint);
        lenient().when(stackV4Endpoint.getSaltPasswordStatus(0L, CRN)).thenReturn(saltPasswordStatus);
    }

    @Test
    void otherFlowRunning() {
        when(flowLogService.isOtherFlowRunning(CLUSTER_ID)).thenReturn(true);

        underTest.executeJob(context);

        verifySkip();
    }

    @Test
    void nullCrn() {
        underTest.setRemoteResourceCrn(null);

        underTest.executeJob(context);

        verify(jobService).unschedule(jobKey);
    }

    @Test
    void emptyCluster() {
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.empty());

        underTest.executeJob(context);

        verify(jobService).unschedule(jobKey);
    }

    @EnumSource(value = DatalakeStatusEnum.class, names = {"STACK_DELETION_IN_PROGRESS", "PROVISIONING_FAILED", "DELETED_ON_PROVIDER_SIDE"})
    @ParameterizedTest
    void unscheduleStatuses(DatalakeStatusEnum status) {
        setStatus(status);

        underTest.executeJob(context);

        verifyNoInteractions(cloudbreakInternalCrnClient);
        verify(jobService).unschedule(jobKey);
    }

    @Test
    void stoppedStatus() {
        setStatus(DatalakeStatusEnum.STOPPED);

        underTest.executeJob(context);

        verifySkip();
    }

    @Test
    void statusOk() {
        setStatus(DatalakeStatusEnum.RUNNING);
        when(saltPasswordStatus.getStatus()).thenReturn(SaltPasswordStatus.OK);

        underTest.executeJob(context);

        verifyNoInteractions(sdxService);
    }

    @Test
    void statusInvalid() {
        setStatus(DatalakeStatusEnum.RUNNING);
        when(saltPasswordStatus.getStatus()).thenReturn(SaltPasswordStatus.INVALID);

        underTest.executeJob(context);

        verify(sdxService).rotateSaltPassword(sdxCluster);
    }

    private void setStatus(DatalakeStatusEnum status) {
        when(sdxStatus.getStatus()).thenReturn(status);
    }

    private void verifySkip() {
        verifyNoInteractions(cloudbreakInternalCrnClient);
        verifyNoInteractions(jobService);
    }
}
