package com.sequenceiq.environment.environment.service.freeipa;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.poller.FreeIpaPollerProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
public class FreeIpaPollerServiceTest {

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerProvider freeipaPollerProvider;

    @InjectMocks
    private FreeIpaPollerService underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "attempt", 1);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
    }

    @Test
    void testStopAttachedFreeipaInstancesWhenFreeipaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaService.describe("crn")).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.stopPoller(2L, "crn")).thenReturn(AttemptResults::justFinish);

        underTest.stopAttachedFreeipaInstances(2L, "crn");

        verify(freeIpaService, times(1)).stopFreeIpa("crn");
    }

    @Test
    void testStopAttachedFreeipaInstancesWhenFreeipaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe("crn")).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.stopPoller(2L, "crn")).thenReturn(AttemptResults::justFinish);

        underTest.stopAttachedFreeipaInstances(2L, "crn");

        verify(freeIpaService, times(0)).stopFreeIpa("crn");
    }

    @Test
    void testStartAttachedFreeipaInstancesWhenFreeipaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaService.describe("crn")).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.startPoller(2L, "crn")).thenReturn(AttemptResults::justFinish);

        underTest.startAttachedFreeipaInstances(2L, "crn");

        verify(freeIpaService, times(0)).startFreeIpa("crn");
    }

    @Test
    void testStartAttachedFreeipaInstancesWhenFreeipaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe("crn")).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.startPoller(2L, "crn")).thenReturn(AttemptResults::justFinish);

        underTest.startAttachedFreeipaInstances(2L, "crn");

        verify(freeIpaService, times(1)).startFreeIpa("crn");
    }
}
