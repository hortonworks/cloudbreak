package com.sequenceiq.externalizedcompute.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterStatusRepository;
import com.sequenceiq.externalizedcompute.service.exception.ExternalizedComputeClusterStatusUpdateException;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterStatusServiceTest {

    private static final Long TIMESTAMP = 1000L;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ExternalizedComputeClusterStatusRepository statusRepository;

    @Mock
    private ExternalizedComputeClusterRepository clusterRepository;

    @Spy
    private Clock clock;

    @InjectMocks
    private ExternalizedComputeClusterStatusService statusService;

    @Captor
    private ArgumentCaptor<ExternalizedComputeCluster> clusterCaptor;

    @Captor
    private ArgumentCaptor<ExternalizedComputeClusterStatus> statusEntityCaptor;

    private ExternalizedComputeCluster cluster;

    private ExternalizedComputeClusterStatus oldStatus;

    @BeforeEach
    void setUp() throws TransactionService.TransactionExecutionException {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));

        cluster = new ExternalizedComputeCluster();
        cluster.setName("perdos-ext-cluster");
        cluster.setResourceCrn("crn");
        cluster.setId(2L);

        oldStatus = new ExternalizedComputeClusterStatus();
        oldStatus.setCreated(1L);
        oldStatus.setStatusReason("statusreason");
        oldStatus.setId(1L);
        oldStatus.setExternalizedComputeCluster(cluster);
        when(statusRepository.findFirstByExternalizedComputeClusterIsOrderByIdDesc(any(ExternalizedComputeCluster.class))).thenReturn(oldStatus);

        lenient().when(clock.getCurrentTimeMillis()).thenReturn(TIMESTAMP);
        lenient().when(clusterRepository.save(clusterCaptor.capture())).thenReturn(cluster);
        lenient().when(statusRepository.save(statusEntityCaptor.capture())).thenReturn(null);
    }

    @Test
    void setDeleteStatusForCluster() throws Exception {
        when(clusterRepository.findById(eq(2L))).thenReturn(Optional.of(cluster));
        oldStatus.setStatus(ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS);
        ExternalizedComputeClusterStatusEnum status = ExternalizedComputeClusterStatusEnum.DELETED;
        statusService.setStatus(cluster, status, "deleted");
        verify(clusterRepository).findById(eq(2L));
        verify(statusRepository, times(2)).findFirstByExternalizedComputeClusterIsOrderByIdDesc(cluster);
        verify(statusRepository).save(any(ExternalizedComputeClusterStatus.class));
        assertEquals(status, statusEntityCaptor.getValue().getStatus());
        assertEquals(TIMESTAMP, clusterCaptor.getValue().getDeleted());
        verify(transactionService).required(any(Runnable.class));
    }

    @Test
    void setStatusForClusterButItIsDeleted() throws Exception {
        oldStatus.setStatus(ExternalizedComputeClusterStatusEnum.DELETED);
        ExternalizedComputeClusterStatusEnum status = ExternalizedComputeClusterStatusEnum.CREATE_IN_PROGRESS;
        ExternalizedComputeClusterStatusUpdateException exception = assertThrows(ExternalizedComputeClusterStatusUpdateException.class,
                () -> statusService.setStatus(cluster, status, "creation in progress"));
        assertEquals("Can't update Externalized Compute Cluster status from DELETED to CREATE_IN_PROGRESS", exception.getMessage());
        verify(clusterRepository, never()).findById(eq(2L));
        verify(statusRepository).findFirstByExternalizedComputeClusterIsOrderByIdDesc(cluster);
        verify(statusRepository, times(0)).save(any(ExternalizedComputeClusterStatus.class));
    }

    @Test
    void setStatusForClusterButItIsDeleteInProgress() {
        oldStatus.setStatus(ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS);
        ExternalizedComputeClusterStatusEnum status = ExternalizedComputeClusterStatusEnum.CREATE_IN_PROGRESS;

        ExternalizedComputeClusterStatusUpdateException exception = assertThrows(ExternalizedComputeClusterStatusUpdateException.class,
                () ->  statusService.setStatus(cluster, status, "creation in progress"));

        verify(statusRepository, times(1)).findFirstByExternalizedComputeClusterIsOrderByIdDesc(cluster);
        verify(statusRepository, times(0)).save(any(ExternalizedComputeClusterStatus.class));
        assertEquals("Can't update Externalized Compute Cluster status from DELETE_IN_PROGRESS to CREATE_IN_PROGRESS",
                exception.getMessage());
    }

    @Test
    void setFailedStatusForClusterButItIsDeleted() {
        oldStatus.setStatus(ExternalizedComputeClusterStatusEnum.DELETED);
        ExternalizedComputeClusterStatusEnum status = ExternalizedComputeClusterStatusEnum.CREATE_FAILED;
        ExternalizedComputeClusterStatusUpdateException exception = assertThrows(ExternalizedComputeClusterStatusUpdateException.class,
                () -> statusService.setStatus(cluster, status, "creation in progress"));
        assertEquals("Can't update Externalized Compute Cluster status from DELETED to CREATE_FAILED", exception.getMessage());
        verify(clusterRepository, never()).findById(eq(2L));
        verify(statusRepository).findFirstByExternalizedComputeClusterIsOrderByIdDesc(cluster);
        verify(statusRepository, times(0)).save(any(ExternalizedComputeClusterStatus.class));
    }

    @Test
    void setFailedStatusForCluster() throws Exception {
        oldStatus.setStatus(ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS);
        ExternalizedComputeClusterStatusEnum status = ExternalizedComputeClusterStatusEnum.DELETE_FAILED;
        statusService.setStatus(cluster, status, "delete failed");
        verify(statusRepository, times(2)).findFirstByExternalizedComputeClusterIsOrderByIdDesc(cluster);
        verify(statusRepository).save(any(ExternalizedComputeClusterStatus.class));
        assertEquals(status, statusEntityCaptor.getValue().getStatus());
        verify(transactionService).required(any(Runnable.class));
    }
}