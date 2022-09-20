package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;

@ExtendWith(MockitoExtension.class)
class FinalizationCleanUpServiceTest {

    private static final Long TEST_STACK_ID = 1L;

    private static final Long TEST_CLUSTER_ID = 2L;

    @Mock
    private LegacyStructuredEventDBService mockStructuredEventDBService;

    @Mock
    private ClusterComponentConfigProvider mockClusterComponentConfigProvider;

    @Mock
    private ClusterService mockClusterService;

    @Mock
    private TransactionService mockTransactionService;

    private FinalizationCleanUpService underTest;

    @BeforeEach
    void setUp() {
        underTest  = new FinalizationCleanUpService(mockStructuredEventDBService, mockTransactionService, mockClusterComponentConfigProvider,
                mockClusterService);
    }

    @Test
    void testDetachClusterComponentRelatedAuditEntriesWhenNoExceptionHappens() throws TransactionExecutionException {
        when(mockClusterService.findClusterIdByStackId(TEST_STACK_ID)).thenReturn(Optional.of(TEST_CLUSTER_ID));
        underTest.detachClusterComponentRelatedAuditEntries(TEST_STACK_ID);

        verify(mockTransactionService, times(1)).required(any(Runnable.class));
        verifyNoMoreInteractions(mockTransactionService);
        verify(mockClusterService, times(1)).findClusterIdByStackId(TEST_STACK_ID);
        verifyNoMoreInteractions(mockClusterService);
    }

    @Test
    void testDetachClusterComponentRelatedAuditEntriesWhenNoClusterIdFound() throws TransactionExecutionException {
        when(mockClusterService.findClusterIdByStackId(TEST_STACK_ID)).thenReturn(Optional.empty());
        underTest.detachClusterComponentRelatedAuditEntries(TEST_STACK_ID);

        verify(mockTransactionService, never()).required(any(Runnable.class));
        verify(mockClusterService, times(1)).findClusterIdByStackId(TEST_STACK_ID);
        verifyNoMoreInteractions(mockClusterService);
    }

    @Test
    void testDetachClusterComponentRelatedAuditEntriesWhenExceptionHappens() throws TransactionExecutionException {
        when(mockClusterService.findClusterIdByStackId(TEST_STACK_ID)).thenReturn(Optional.of(TEST_CLUSTER_ID));
        Exception expectedExceptionCause = new RuntimeException();
        doThrow(expectedExceptionCause).when(mockTransactionService).required(any(Runnable.class));

        CleanUpException expectedException = assertThrows(CleanUpException.class, () -> underTest.detachClusterComponentRelatedAuditEntries(TEST_STACK_ID));

        assertEquals(expectedExceptionCause, expectedException.getCause());
        assertEquals(ClusterComponentHistory.class.getSimpleName() + " cleanup has failed!", expectedException.getMessage());
        verify(mockTransactionService, times(1)).required(any(Runnable.class));
        verifyNoMoreInteractions(mockTransactionService);
    }

    @Test
    void testCleanUpStructuredEventsWhenLegacyStructuredEventDBServiceThrowsException() throws TransactionExecutionException {
        Exception expectedExceptionCause = new RuntimeException();
        doThrow(expectedExceptionCause).when(mockTransactionService).required(any(Runnable.class));

        CleanUpException expectedException = assertThrows(CleanUpException.class, () -> underTest.cleanUpStructuredEventsForStack(TEST_STACK_ID));

        assertEquals(StructuredEventEntity.class.getSimpleName() + " cleanup has failed!", expectedException.getMessage());
        assertEquals(expectedExceptionCause, expectedException.getCause());
    }

    @Test
    void testCleanUpStructuredEventsWhenEverythingGoesWell() throws TransactionExecutionException {
        underTest.cleanUpStructuredEventsForStack(TEST_STACK_ID);

        verify(mockTransactionService, times(1)).required(any(Runnable.class));
    }

}