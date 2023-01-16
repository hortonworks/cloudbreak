package com.sequenceiq.datalake.service.sdx.flowwait;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskForCloudbreakFlow;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskService;
import com.sequenceiq.datalake.service.sdx.flowwait.task.factory.SdxWaitTaskFactory;

@ExtendWith(MockitoExtension.class)
class SdxWaitServiceTest {

    private static final long SDX_ID = 1L;

    private static final String POLLING_MESSAGE = "polling";

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxWaitTaskService sdxWaitTaskService;

    @Mock
    private SdxWaitTaskFactory sdxWaitTaskFactory;

    @InjectMocks
    private SdxWaitService underTest;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private SdxWaitTaskForCloudbreakFlow sdxWaitTaskForCloudbreakFlow;

    @Mock
    private PollingConfig pollingConfig;

    @BeforeEach
    void setUp() {
        lenient().when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        lenient().when(sdxWaitTaskFactory.createCloudbreakFlowWaitTask(sdxCluster, pollingConfig, POLLING_MESSAGE)).thenReturn(sdxWaitTaskForCloudbreakFlow);
    }

    @Test
    void waitForCloudbreakFlowById() {
        underTest.waitForCloudbreakFlow(SDX_ID, pollingConfig, POLLING_MESSAGE);

        verify(sdxService).getById(SDX_ID);
        verify(sdxWaitTaskFactory).createCloudbreakFlowWaitTask(sdxCluster, pollingConfig, POLLING_MESSAGE);
        verify(sdxWaitTaskService).waitFor(sdxWaitTaskForCloudbreakFlow);
    }

    @Test
    void waitForCloudbreakFlowByCluster() {
        underTest.waitForCloudbreakFlow(sdxCluster, pollingConfig, POLLING_MESSAGE);

        verifyNoInteractions(sdxService);
        verify(sdxWaitTaskFactory).createCloudbreakFlowWaitTask(sdxCluster, pollingConfig, POLLING_MESSAGE);
        verify(sdxWaitTaskService).waitFor(sdxWaitTaskForCloudbreakFlow);
    }

}
