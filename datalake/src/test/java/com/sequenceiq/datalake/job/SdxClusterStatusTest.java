package com.sequenceiq.datalake.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SdxClusterStatusTest.TestAppContext.class)
class SdxClusterStatusTest {

    private static final Long SDX_ID = 456L;

    private static final Long STACK_ID = 123L;

    @Inject
    private SdxClusterStatusCheckerJob underTest;

    @MockBean
    private SdxClusterRepository sdxClusterRepository;

    @MockBean
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @MockBean
    private SdxStatusService sdxStatusService;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private AutoscaleV4Endpoint autoscaleV4Endpoint;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private SdxCluster sdxCluster;

    private StackStatusV4Response stack;

    private SdxStatusEntity status;

    @BeforeEach
    void setUp() {
        underTest.setLocalId(SDX_ID.toString());
        underTest.setRemoteResourceCrn(STACK_ID.toString());

        sdxCluster = new SdxCluster();
        when(sdxClusterRepository.findById(SDX_ID)).thenReturn(Optional.of(sdxCluster));

        stack = new StackStatusV4Response();
        when(cloudbreakInternalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
        when(autoscaleV4Endpoint.getStatusByCrn(STACK_ID.toString())).thenReturn(stack);

        status = new SdxStatusEntity();
        status.setDatalake(sdxCluster);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
    }

    @Test
    void available() {
        setUpSdxStatus(DatalakeStatusEnum.RUNNING);
        stack.setStatus(Status.AVAILABLE);

        underTest.executeInternal(jobExecutionContext);

        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(any(), any(), anyString(), eq(sdxCluster));
    }

    private void setUpSdxStatus(DatalakeStatusEnum status) {
        this.status.setStatus(status);
    }

    @Configuration
    @Import({
            SdxClusterStatusCheckerJob.class
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private StatusCheckerJobService jobService;

    }
}
