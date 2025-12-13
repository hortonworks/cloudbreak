package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.repository.StackStatusRepository;

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
        verify(stackStatusRepository, times(1)).findFirstByStackIdOrderByCreatedDesc(eq(STACK_ID));
    }

    @Test
    void testCountStacksByStatusAndCloudPlatform() {
        underTest.countStacksByStatusAndCloudPlatform(CLOUD_PLATFORM);
        verify(stackStatusRepository, times(1)).countStacksByStatusAndCloudPlatform(eq(CLOUD_PLATFORM));
    }

    @Test
    void testCountStacksByStatusAndTunnel() {
        underTest.countStacksByStatusAndTunnel(TUNNEL);
        verify(stackStatusRepository, times(1)).countStacksByStatusAndTunnel(eq(TUNNEL));
    }

    @Test
    void testCleanupByPreservedStatus() {
        Status preservedStatus = Status.DELETE_COMPLETED;
        underTest.cleanupByPreservedStatus(1L, preservedStatus);
        verify(stackStatusRepository).deleteAllByStackIdAndStatusNot(anyLong(), eq(preservedStatus));
    }

    @Test
    void testCleanupByTimestamp() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "statusCleanupLimit", 100, true);
        Stack stack = mock(Stack.class);
        List<StackStatus> statusList = Stream
                .generate(() -> new StackStatus(stack, Status.AVAILABLE, "", DetailedStackStatus.AVAILABLE))
                .limit(500)
                .toList();
        when(stackStatusRepository.findAllByStackIdOrderByCreatedDesc(anyLong())).thenReturn(statusList);

        underTest.cleanupByStackId(1L);

        ArgumentCaptor<List<StackStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(stackStatusRepository).deleteAllInBatch(captor.capture());
        assertEquals(400, captor.getValue().size());
    }
}