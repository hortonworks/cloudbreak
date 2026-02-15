package com.sequenceiq.environment.environment.service.freeipa;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.poller.FreeIpaPollerProvider;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

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
        ReflectionTestUtils.setField(underTest, "startStopAttempt", 1);
        ReflectionTestUtils.setField(underTest, "upgradeccmAttempt", 1);
        ReflectionTestUtils.setField(underTest, "startStopSleeptime", 1);
        ReflectionTestUtils.setField(underTest, "upgradeccmSleeptime", 1);
    }

    @Test
    void testStopAttachedFreeipaInstancesWhenFreeipaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.stopPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.stopAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(1)).stopFreeIpa(ENV_CRN);
    }

    @Test
    void testStopAttachedFreeipaInstancesWhenFreeipaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.stopPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.stopAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(0)).stopFreeIpa(ENV_CRN);
    }

    @Test
    void testStartAttachedFreeipaInstancesWhenFreeipaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.startPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.startAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(0)).startFreeIpa(ENV_CRN);
    }

    @Test
    void testStartAttachedFreeipaInstancesWhenFreeipaStopped() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));
        when(freeipaPollerProvider.startPoller(ENV_ID, ENV_CRN)).thenReturn(AttemptResults::justFinish);

        underTest.startAttachedFreeipaInstances(ENV_ID, ENV_CRN);

        verify(freeIpaService, times(1)).startFreeIpa(ENV_CRN);
    }

    @Test
    void testSyncUsersWhenFreeIpaAvailable() {
        DescribeFreeIpaResponse freeipaResponse = new DescribeFreeIpaResponse();
        freeipaResponse.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
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
        freeipaResponse.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        freeipaResponse.setStatus(Status.STOPPED);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(freeipaResponse));

        underTest.waitForSynchronizeUsers(ENV_ID, ENV_CRN);

        verify(freeIpaService, never()).synchronizeAllUsersInEnvironment(ENV_CRN);
    }

    @Test
    void testWaitForCcmUpgrade() {
        OperationStatus status = new OperationStatus("123", OperationType.UPGRADE_CCM, OperationState.REQUESTED, null, null, null, 0, null);
        when(freeIpaService.upgradeCcm(any())).thenReturn(status);
        when(freeipaPollerProvider.upgradeCcmPoller(ENV_ID, ENV_CRN, "123")).thenReturn(AttemptResults.justFinish());

        underTest.waitForCcmUpgrade(ENV_ID, ENV_CRN);

        verify(freeIpaService).upgradeCcm(ENV_CRN);
    }

    @Test
    void testWaitForCcmUpgradeAlreadyCompleted() {
        OperationStatus status = new OperationStatus("123", OperationType.UPGRADE_CCM, OperationState.COMPLETED, null, null, null, 0, null);
        when(freeIpaService.upgradeCcm(any())).thenReturn(status);

        underTest.waitForCcmUpgrade(ENV_ID, ENV_CRN);

        verify(freeIpaService).upgradeCcm(ENV_CRN);
        verify(freeipaPollerProvider, never()).upgradeCcmPoller(any(), any(), any());
    }

    @Test
    void testWaitForCcmUpgradeFailed() {
        OperationStatus status = new OperationStatus("123", OperationType.UPGRADE_CCM, OperationState.REQUESTED, null, null, null, 0, null);
        when(freeIpaService.upgradeCcm(any())).thenReturn(status);
        when(freeipaPollerProvider.upgradeCcmPoller(ENV_ID, ENV_CRN, "123")).thenThrow(new RuntimeException("error"));

        assertThatThrownBy(() -> underTest.waitForCcmUpgrade(ENV_ID, ENV_CRN))
                .hasMessageContaining("FreeIPA upgrade of Cluster Connectivity Manager timed out or error happened")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);

        verify(freeIpaService).upgradeCcm(ENV_CRN);
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
