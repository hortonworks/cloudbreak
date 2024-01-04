package com.sequenceiq.freeipa.service.stack;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.Tunnel;
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
}