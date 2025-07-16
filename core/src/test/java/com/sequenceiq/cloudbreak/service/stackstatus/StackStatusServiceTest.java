package com.sequenceiq.cloudbreak.service.stackstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.repository.StackStatusRepository;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class StackStatusServiceTest {

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final Tunnel TUNNEL = Tunnel.CCM;

    @Mock
    private StackStatusRepository stackStatusRepository;

    @InjectMocks
    private StackStatusService underTest;

    @Test
    void testFindFirstByStackIdOrderByCreatedDesc() {
        when(stackStatusRepository.findFirstByStackIdOrderByCreatedDesc(eq(STACK_ID))).thenReturn(Optional.of(new StackStatus()));
        underTest.findFirstByStackIdOrderByCreatedDesc(STACK_ID);
        verify(stackStatusRepository, Mockito.times(1)).findFirstByStackIdOrderByCreatedDesc(eq(STACK_ID));
    }

    @Test
    void testCountStacksByStatusAndCloudPlatform() {
        underTest.countStacksByStatusAndCloudPlatform(CLOUD_PLATFORM);
        verify(stackStatusRepository, Mockito.times(1)).countStacksByStatusAndCloudPlatform(eq(CLOUD_PLATFORM));
    }

    @Test
    void testCountStacksByStatusAndTunnel() {
        underTest.countStacksByStatusAndTunnel(TUNNEL);
        verify(stackStatusRepository, Mockito.times(1)).countStacksByStatusAndTunnel(eq(TUNNEL));
    }

    @Test
    void testCleanupByPreservedStatus() {
        Status preservedStatus = Status.DELETE_COMPLETED;
        underTest.cleanupByPreservedStatus(1L, preservedStatus);
        verify(stackStatusRepository).deleteAllByStackIdAndStatusNot(anyLong(), eq(preservedStatus));
    }

    @Test
    void testCleanupByTimestamp() {
        int limit = 100;
        long timestamp = System.currentTimeMillis();
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<StackStatus> mockedPage = mock(Page.class);
        when(mockedPage.getContent()).thenReturn(List.of());
        when(stackStatusRepository.findAllByCreatedLessThan(anyLong(), any())).thenReturn(mockedPage);
        underTest.cleanupByTimestamp(limit, timestamp);
        verify(stackStatusRepository).findAllByCreatedLessThan(eq(timestamp), pageCaptor.capture());
        assertEquals(limit, pageCaptor.getValue().getPageSize());
        verify(stackStatusRepository).deleteAll(any());
    }
}