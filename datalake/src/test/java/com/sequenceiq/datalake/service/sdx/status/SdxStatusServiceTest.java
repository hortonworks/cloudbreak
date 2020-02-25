package com.sequenceiq.datalake.service.sdx.status;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;

@ExtendWith(MockitoExtension.class)
class SdxStatusServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private SdxNotificationService sdxNotificationService;

    @Mock
    private SdxStatusRepository sdxStatusRepository;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Spy
    private Clock clock;

    @InjectMocks
    private SdxStatusService sdxStatusService;

    @Test
    void setStatusForDatalakeAndNotify() throws TransactionService.TransactionExecutionException {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));

        long deletedTimeStamp = 100L;
        when(clock.getCurrentTimeMillis()).thenReturn(deletedTimeStamp);

        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setRuntime("7.0.2");
        sdxCluster.setClusterName("datalake-cluster");
        sdxCluster.setId(2L);

        SdxStatusEntity oldStatus = new SdxStatusEntity();
        oldStatus.setStatus(DatalakeStatusEnum.STACK_DELETED);
        oldStatus.setCreated(1L);
        oldStatus.setStatusReason("stack deleted");
        oldStatus.setId(1L);
        oldStatus.setDatalake(sdxCluster);
        when(sdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(any(SdxCluster.class))).thenReturn(oldStatus);

        when(sdxClusterRepository.findById(eq(2L))).thenReturn(Optional.of(sdxCluster));
        ArgumentCaptor<SdxStatusEntity> statusEntityCaptor = ArgumentCaptor.forClass(SdxStatusEntity.class);
        when(sdxStatusRepository.save(statusEntityCaptor.capture())).thenReturn(null);
        ArgumentCaptor<SdxCluster> sdxClusterCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxClusterRepository.save(sdxClusterCaptor.capture())).thenReturn(sdxCluster);

        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETED, ResourceEvent.SDX_RDS_DELETION_FINISHED, "deleted", sdxCluster);
        verify(sdxStatusRepository, times(1)).save(any(SdxStatusEntity.class));
        assertEquals(DatalakeStatusEnum.DELETED, statusEntityCaptor.getValue().getStatus());
        assertEquals(Long.valueOf(deletedTimeStamp), sdxClusterCaptor.getValue().getDeleted());
        verify(transactionService, times(1)).required(any(Runnable.class));
    }
}