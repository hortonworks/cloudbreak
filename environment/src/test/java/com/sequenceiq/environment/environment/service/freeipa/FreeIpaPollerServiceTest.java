package com.sequenceiq.environment.environment.service.freeipa;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.poller.FreeIpaPollerProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

@ExtendWith(MockitoExtension.class)
public class FreeIpaPollerServiceTest {

    private static final String ENV_CRN = "crn";

    private static final long ENV_ID = 2L;

    private static final String OPERATION = "operation";

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
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.stopPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.stopAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(1)).stopFreeIpa(ENV_CRN);
    }

    @Test
    void testStopAttachedFreeipaInstancesWhenFreeipaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.stopPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.stopAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(0)).stopFreeIpa(ENV_CRN);
    }

    @Test
    void testStartAttachedFreeipaInstancesWhenFreeipaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.startPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.startAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(0)).startFreeIpa(ENV_CRN);
    }

    @Test
    void testStartAttachedFreeipaInstancesWhenFreeipaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.startPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.startAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(1)).startFreeIpa(ENV_CRN);
    }

    @Test
    void testSyncUsersWhenFreeIpaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeIpaService.synchronizeAllUsersInEnvironment(ENV_CRN)).thenReturn(createStatus(SynchronizationStatus.REQUESTED, ""));
        when(freeipaPollerProvider.syncUsersPoller(ENV_ID, ENV_CRN, OPERATION)).thenReturn(AttemptResults::justFinish);

        underTest.waitForSynchronizeUsers(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(1)).synchronizeAllUsersInEnvironment(ENV_CRN);
    }

    @Test
    void testSyncUsersWhenFreeIpaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));

        underTest.waitForSynchronizeUsers(ENV_ID, ENV_CRN);

        verify(freeIpaService, never()).synchronizeAllUsersInEnvironment(ENV_CRN);
    }

    private static SyncOperationStatus createStatus(SynchronizationStatus syncStatus, String error) {
        List<FailureDetails> failureDetails = new ArrayList<>();
        if (StringUtils.isNotBlank(error)) {
            failureDetails.add(new FailureDetails(Long.toString(ENV_ID), error));
        }
        return new SyncOperationStatus(OPERATION, SyncOperationType.USER_SYNC, syncStatus,
                List.of(), failureDetails, error, System.currentTimeMillis(), null);
    }
}
