package com.sequenceiq.environment.environment.poller;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.START_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

class FreeIpaPollerProviderTest {

    private static final Long ENV_ID = 1000L;

    private static final String CRN = "crn";

    private static final String OPERATION_ID = "operationId";

    private final FreeIpaService freeIpaService = Mockito.mock(FreeIpaService.class);

    private final FreeIpaPollerProvider underTest = new FreeIpaPollerProvider(freeIpaService);

    @ParameterizedTest
    @MethodSource("freeIpaStopStatuses")
    void testStopPoller(Status s1Status, AttemptState attemptState, String message) throws Exception {
        DescribeFreeIpaResponse stack1 = getDescribeFreeIpaResponse(s1Status, CRN);

        when(freeIpaService.describe(CRN)).thenReturn(Optional.ofNullable(stack1));

        AttemptResult<Void> result = underTest.stopPoller(ENV_ID, CRN).process();

        Assertions.assertEquals(attemptState, result.getState());
        Assertions.assertEquals(message, result.getMessage());
    }

    @ParameterizedTest
    @MethodSource("freeIpaStartStatuses")
    void testStartPoller(Status s1Status, AttemptState attemptState, String message) throws Exception {
        DescribeFreeIpaResponse stack1 = getDescribeFreeIpaResponse(s1Status, CRN);

        when(freeIpaService.describe(CRN)).thenReturn(Optional.ofNullable(stack1));

        AttemptResult<Void> result = underTest.startPoller(ENV_ID, CRN).process();

        Assertions.assertEquals(attemptState, result.getState());
        Assertions.assertEquals(message, result.getMessage());
    }

    @Test
    void testStartPollerWhenFreeIpaNull() throws Exception {
        when(freeIpaService.describe(CRN)).thenReturn(Optional.empty());

        AttemptResult<Void> result = underTest.startPoller(ENV_ID, CRN).process();

        Assertions.assertEquals(AttemptState.FINISH, result.getState());
    }

    @ParameterizedTest
    @MethodSource("freeIpaSyncStatuses")
    void testSyncUsersPoller(SyncOperationStatus syncStatus, AttemptState attemptState, String message) throws Exception {
        when(freeIpaService.getSyncOperationStatus(CRN, OPERATION_ID)).thenReturn(syncStatus);

        AttemptResult<Void> result = underTest.syncUsersPoller(ENV_ID, CRN, OPERATION_ID).process();

        Assertions.assertEquals(attemptState, result.getState());
        Assertions.assertEquals(message, result.getMessage());
    }

    private static Stream<Arguments> freeIpaStopStatuses() {
        return Stream.of(
                Arguments.of(STOPPED, AttemptState.FINISH, ""),
                Arguments.of(STOP_IN_PROGRESS, AttemptState.CONTINUE, ""),
                Arguments.of(STOP_FAILED, AttemptState.BREAK, "FreeIpa stop failed 'crn', reason")
        );
    }

    private static Stream<Arguments> freeIpaStartStatuses() {
        return Stream.of(
                Arguments.of(AVAILABLE, AttemptState.FINISH, ""),
                Arguments.of(UPDATE_IN_PROGRESS, AttemptState.CONTINUE, ""),
                Arguments.of(START_FAILED, AttemptState.BREAK, "FreeIpa start failed 'crn', reason")
        );
    }

    private static Stream<Arguments> freeIpaSyncStatuses() {
        return Stream.of(
                Arguments.of(createStatus(SynchronizationStatus.REQUESTED, ""), AttemptState.CONTINUE, ""),
                Arguments.of(createStatus(SynchronizationStatus.RUNNING, ""), AttemptState.CONTINUE, ""),
                Arguments.of(createStatus(SynchronizationStatus.COMPLETED, ""), AttemptState.FINISH, ""),
                Arguments.of(createStatus(SynchronizationStatus.FAILED, "failed"), AttemptState.BREAK, getSyncErrorMessage("failed")),
                Arguments.of(createStatus(SynchronizationStatus.REJECTED, "rejected"), AttemptState.BREAK, getSyncErrorMessage("rejected")),
                Arguments.of(createStatus(SynchronizationStatus.TIMEDOUT, "timeout"), AttemptState.BREAK, getSyncErrorMessage("timeout"))
        );
    }

    private static String getSyncErrorMessage(String error) {
        return String.format("FreeIpa user synchronization failed '%s', [FailureDetails{environment='%s', message='%s'additionalDetails='{}'}]",
                OPERATION_ID, ENV_ID, error);
    }

    private DescribeFreeIpaResponse getDescribeFreeIpaResponse(Status status, String name) {
        DescribeFreeIpaResponse stack1 = new DescribeFreeIpaResponse();
        stack1.setStatus(status);
        stack1.setName(name);
        stack1.setCrn(name);
        stack1.setStatusReason("reason");
        return stack1;
    }

    private static SyncOperationStatus createStatus(SynchronizationStatus syncStatus, String error) {
        List<FailureDetails> failureDetails = new ArrayList<>();
        if (StringUtils.isNotBlank(error)) {
            failureDetails.add(new FailureDetails(ENV_ID.toString(), error));
        }
        return new SyncOperationStatus(OPERATION_ID, SyncOperationType.USER_SYNC, syncStatus,
                List.of(), failureDetails, error, System.currentTimeMillis(), null);
    }
}
