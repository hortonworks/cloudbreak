package com.sequenceiq.environment.environment.poller;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.START_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPDATE_IN_PROGRESS;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

class FreeIpaPollerProviderTest {

    private static final Long ENV_ID = 1000L;

    private final FreeIpaService freeIpaService = Mockito.mock(FreeIpaService.class);

    private final FreeIpaPollerProvider underTest = new FreeIpaPollerProvider(freeIpaService);

    @ParameterizedTest
    @MethodSource("freeipaStopStatuses")
    void testStopPoller(Status s1Status, AttemptState attemptState, String message) throws Exception {
        String crn = "crn";
        DescribeFreeIpaResponse stack1 = getDescribeFreeIpaResponse(s1Status, crn);

        Mockito.when(freeIpaService.describe(crn)).thenReturn(Optional.ofNullable(stack1));

        AttemptResult<Void> result = underTest.stopPoller(ENV_ID, crn).process();

        Assertions.assertEquals(attemptState, result.getState());
        Assertions.assertEquals(message, result.getMessage());
    }

    @ParameterizedTest
    @MethodSource("freeipaStartStatuses")
    void testStartPoller(Status s1Status, AttemptState attemptState, String message) throws Exception {
        String crn = "crn";
        DescribeFreeIpaResponse stack1 = getDescribeFreeIpaResponse(s1Status, crn);

        Mockito.when(freeIpaService.describe(crn)).thenReturn(Optional.ofNullable(stack1));

        AttemptResult<Void> result = underTest.startPoller(ENV_ID, crn).process();

        Assertions.assertEquals(attemptState, result.getState());
        Assertions.assertEquals(message, result.getMessage());
    }

    @Test
    void testStartPollerWhenFreeipaNull() throws Exception {
        String crn = "crn";
        Mockito.when(freeIpaService.describe(crn)).thenReturn(Optional.empty());

        AttemptResult<Void> result = underTest.startPoller(ENV_ID, crn).process();

        Assertions.assertEquals(AttemptState.FINISH, result.getState());
    }

    private static Stream<Arguments> freeipaStopStatuses() {
        return Stream.of(
                Arguments.of(STOPPED, AttemptState.FINISH, ""),
                Arguments.of(STOP_IN_PROGRESS, AttemptState.CONTINUE, ""),
                Arguments.of(STOP_FAILED, AttemptState.BREAK, "Freeipa stop failed 'crn', reason")
        );
    }

    private static Stream<Arguments> freeipaStartStatuses() {
        return Stream.of(
                Arguments.of(AVAILABLE, AttemptState.FINISH, ""),
                Arguments.of(UPDATE_IN_PROGRESS, AttemptState.CONTINUE, ""),
                Arguments.of(START_FAILED, AttemptState.BREAK, "Freeipa start failed 'crn', reason")
        );
    }

    private DescribeFreeIpaResponse getDescribeFreeIpaResponse(Status status, String name) {
        DescribeFreeIpaResponse stack1 = new DescribeFreeIpaResponse();
        stack1.setStatus(status);
        stack1.setName(name);
        stack1.setCrn(name);
        stack1.setStatusReason("reason");
        return stack1;
    }
}
