package com.sequenceiq.cloudbreak.service.stack.flow;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;

@ExtendWith(MockitoExtension.class)
class FinalizationCleanUpServiceTest {

    @Mock
    private LegacyStructuredEventDBService mockStructuredEventDBService;

    @Mock
    private ClusterComponentConfigProvider mockClusterComponentConfigProvider;

    @Mock
    private ClusterService mockClusterService;

    @InjectMocks
    private FinalizationCleanUpService underTest;

    @Test
    void testDetachClusterComponentRelatedAuditEntriesWhenNoExceptionHappens() {
        Set<Long> deletedClusterIds = Set.of(1L);
        when(mockClusterService.findIdsByDeletedStatus()).thenReturn(deletedClusterIds);
        underTest.detachClusterComponentRelatedAuditEntries();

        verify(mockClusterComponentConfigProvider, times(1)).cleanUpDetachedEntries(deletedClusterIds);
        verifyNoMoreInteractions(mockClusterComponentConfigProvider);
        verify(mockClusterService, times(1)).findIdsByDeletedStatus();
    }

    @Test
    void testDetachClusterComponentRelatedAuditEntriesWhenExceptionHappens() {
        Exception expectedExceptionCause = new RuntimeException();
        doThrow(expectedExceptionCause).when(mockClusterComponentConfigProvider).cleanUpDetachedEntries(emptySet());

        CleanUpException expectedException = assertThrows(CleanUpException.class, () -> underTest.detachClusterComponentRelatedAuditEntries());

        assertEquals(expectedExceptionCause, expectedException.getCause());
        assertEquals(ClusterComponentHistory.class.getSimpleName() + " cleanup has failed!", expectedException.getMessage());

        verify(mockClusterComponentConfigProvider, times(1)).cleanUpDetachedEntries(emptySet());
    }

    @Test
    void testCleanUpStructuredEventsWhenLegacyStructuredEventDBServiceThrowsException() {
        Exception expectedExceptionCause = new RuntimeException();
        doThrow(expectedExceptionCause).when(mockStructuredEventDBService).deleteEntriesByResourceIdsOlderThanThreeMonths(any());

        CleanUpException expectedException = assertThrows(CleanUpException.class, () -> underTest.cleanUpStructuredEventsForStack(1L));

        assertEquals(StructuredEventEntity.class.getSimpleName() + " cleanup has failed!", expectedException.getMessage());
        assertEquals(expectedExceptionCause, expectedException.getCause());

        verify(mockStructuredEventDBService, times(1)).deleteEntriesByResourceIdsOlderThanThreeMonths(any());
        verifyNoMoreInteractions(mockStructuredEventDBService);
    }

    @Test
    void testCleanUpStructuredEventsWhenEverythingGoesWell() {
        underTest.cleanUpStructuredEventsForStack(1L);

        verify(mockStructuredEventDBService, times(1)).deleteEntriesByResourceIdsOlderThanThreeMonths(any());
        verifyNoMoreInteractions(mockStructuredEventDBService);
    }

}