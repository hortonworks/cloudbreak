package com.sequenceiq.environment.environment.service.freeipa;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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
        ReflectionTestUtils.setField(underTest, "modifyUserDefinedTagsAttempt", 1);
        ReflectionTestUtils.setField(underTest, "startStopSleeptime", 1);
        ReflectionTestUtils.setField(underTest, "upgradeccmSleeptime", 1);
        ReflectionTestUtils.setField(underTest, "modifyUserDefinedTagsSleeptime", 1);
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

    @Test
    void testWaitForModifyUserDefinedTags() {
        Map<String, String> userDefinedTags = Map.of("custom", "value");
        OperationStatus status = tagsOperationStatus(OperationState.REQUESTED);
        when(freeIpaService.triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags)).thenReturn(status);
        when(freeipaPollerProvider.modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123")).thenReturn(AttemptResults.justFinish());

        underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, userDefinedTags, null);

        verify(freeIpaService).triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags);
    }

    @Test
    void testWaitForModifyUserDefinedTagsAlreadyCompleted() {
        Map<String, String> userDefinedTags = Map.of("custom", "value");
        OperationStatus status = tagsOperationStatus(OperationState.COMPLETED);
        when(freeIpaService.triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags)).thenReturn(status);

        underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, userDefinedTags, null);

        verify(freeIpaService).triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags);
        verify(freeipaPollerProvider, never()).modifyUserDefinedTagsPoller(any(), any(), any());
    }

    @Test
    void testWaitForModifyUserDefinedTagsFailed() {
        Map<String, String> userDefinedTags = Map.of("custom", "value");
        OperationStatus status = tagsOperationStatus(OperationState.REQUESTED);
        when(freeIpaService.triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags)).thenReturn(status);
        when(freeipaPollerProvider.modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123")).thenThrow(new RuntimeException("error"));

        assertThatThrownBy(() -> underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, userDefinedTags, null))
                .hasMessageContaining("FreeIPA user defined tags update timed out or error happened")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);

        verify(freeIpaService).triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags);
    }

    @Test
    void testWaitForDeleteUserDefinedTags() {
        Set<String> tagKeys = Set.of("custom");
        OperationStatus status = tagsOperationStatus(OperationState.REQUESTED);
        when(freeIpaService.triggerUserDefinedTagsDelete(ENV_CRN, tagKeys)).thenReturn(status);
        when(freeipaPollerProvider.modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123")).thenReturn(AttemptResults.justFinish());

        underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, Map.of(), tagKeys);

        verify(freeIpaService).triggerUserDefinedTagsDelete(ENV_CRN, tagKeys);
    }

    @Test
    void testWaitForDeleteUserDefinedTagsRetriesTriggerWhileAnotherFlowIsRunning() {
        ReflectionTestUtils.setField(underTest, "modifyUserDefinedTagsAttempt", 2);
        Set<String> tagKeys = Set.of("custom");
        OperationStatus status = tagsOperationStatus(OperationState.REQUESTED);
        when(freeIpaService.triggerUserDefinedTagsDelete(ENV_CRN, tagKeys))
                .thenThrow(new FreeIpaOperationFailedException("conflict", new WebApplicationException(Response.status(Response.Status.CONFLICT).build())))
                .thenReturn(status);
        when(freeipaPollerProvider.modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123")).thenReturn(AttemptResults.justFinish());

        underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, Map.of(), tagKeys);

        verify(freeIpaService, times(2)).triggerUserDefinedTagsDelete(ENV_CRN, tagKeys);
        verify(freeipaPollerProvider).modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123");
    }

    @Test
    void testWaitForDeleteUserDefinedTagsRetriesTriggerWhenOperationIsRejected() {
        ReflectionTestUtils.setField(underTest, "modifyUserDefinedTagsAttempt", 2);
        Set<String> tagKeys = Set.of("custom");
        when(freeIpaService.triggerUserDefinedTagsDelete(ENV_CRN, tagKeys))
                .thenReturn(tagsOperationStatus(OperationState.REJECTED))
                .thenReturn(tagsOperationStatus(OperationState.REQUESTED));
        when(freeipaPollerProvider.modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123")).thenReturn(AttemptResults.justFinish());

        underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, Map.of(), tagKeys);

        verify(freeIpaService, times(2)).triggerUserDefinedTagsDelete(ENV_CRN, tagKeys);
        verify(freeipaPollerProvider).modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123");
    }

    @Test
    void testWaitForDeleteUserDefinedTagsFailsWhenOperationStaysRejected() {
        Set<String> tagKeys = Set.of("custom");
        when(freeIpaService.triggerUserDefinedTagsDelete(ENV_CRN, tagKeys)).thenReturn(tagsOperationStatus(OperationState.REJECTED));

        assertThatThrownBy(() -> underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, Map.of(), tagKeys))
                .hasMessageContaining("Starting FreeIPA user defined tags deletion timed out")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);

        verify(freeipaPollerProvider, never()).modifyUserDefinedTagsPoller(any(), any(), any());
    }

    @Test
    void testWaitForModifyUserDefinedTagsFailsWhenPollerBreaks() {
        Map<String, String> userDefinedTags = Map.of("custom", "value");
        when(freeIpaService.triggerUserDefinedTagsUpdate(ENV_CRN, userDefinedTags)).thenReturn(tagsOperationStatus(OperationState.REQUESTED));
        when(freeipaPollerProvider.modifyUserDefinedTagsPoller(ENV_ID, ENV_CRN, "123"))
                .thenReturn(AttemptResults.breakFor("FreeIPA modify user defined tags operation request was rejected."));

        assertThatThrownBy(() -> underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, userDefinedTags, null))
                .hasMessageContaining("FreeIPA user defined tags update failed")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);
    }

    @Test
    void testWaitForDeleteUserDefinedTagsDoesNotRetryTriggerOnOtherFailure() {
        ReflectionTestUtils.setField(underTest, "modifyUserDefinedTagsAttempt", 2);
        Set<String> tagKeys = Set.of("custom");
        when(freeIpaService.triggerUserDefinedTagsDelete(ENV_CRN, tagKeys))
                .thenThrow(new FreeIpaOperationFailedException("bad request",
                        new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build())));

        assertThatThrownBy(() -> underTest.waitForUserDefinedTagsModification(ENV_ID, ENV_CRN, Map.of(), tagKeys))
                .hasMessageContaining("Starting FreeIPA user defined tags deletion failed")
                .isExactlyInstanceOf(FreeIpaOperationFailedException.class);

        verify(freeIpaService).triggerUserDefinedTagsDelete(ENV_CRN, tagKeys);
        verify(freeipaPollerProvider, never()).modifyUserDefinedTagsPoller(any(), any(), any());
    }

    private static OperationStatus tagsOperationStatus(OperationState state) {
        return new OperationStatus("123", OperationType.MODIFY_USER_DEFINED_TAGS, state, null, null, null, 0, null);
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
