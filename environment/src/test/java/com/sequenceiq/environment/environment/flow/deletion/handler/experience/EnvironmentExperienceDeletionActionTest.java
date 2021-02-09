package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

@ExtendWith(MockitoExtension.class)
class EnvironmentExperienceDeletionActionTest {

    private static final String FAILURE_BASIC_MSG = "Failed to delete Experience!";

    private static final int ONCE = 1;

    @Mock
    private Pair<PollingResult, Exception> mockPollingResultPair;

    @Mock
    private ExperienceConnectorService mockExperienceConnectorService;

    @Mock
    private PollingService<ExperiencePollerObject> mockExperiencePollingService;

    @Mock
    private ExperiencePollingFailureResolver mockExperiencePollingFailureResolver;

    @InjectMocks
    private EnvironmentExperienceDeletionAction underTest;

    @Test
    void testExecuteShouldInvokeDeleteConnectedExperiences() {
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(new ImmutablePair<>(PollingResult.SUCCESS, null));
        underTest.execute(new Environment(), false);

        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any());
        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testExecuteWhenPollingResultWasNotSuccessfulButNoExceptionHasGivenInThePollingResultPairThenExperienceOperationFailedExceptionShouldCome() {
        ImmutablePair<PollingResult, Exception> pollingResult = new ImmutablePair<>(PollingResult.FAILURE, null);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(pollingResult);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.execute(new Environment(), false));

        assertNotNull(expectedException);
        assertEquals(FAILURE_BASIC_MSG, expectedException.getMessage());

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testExecuteWhenPollingResultWasNotSuccessfulAndExceptionHasGivenInThePollingResultPairThenExperienceOperationFailedExceptionShouldCome() {
        String resolvedMessage = "Because of reasons...";
        String expectedExceptionMessage = FAILURE_BASIC_MSG + " " + resolvedMessage;
        ImmutablePair<PollingResult, Exception> pollingResult = new ImmutablePair<>(PollingResult.FAILURE, new Exception());
        when(mockExperiencePollingFailureResolver.getMessageForFailure(pollingResult)).thenReturn(resolvedMessage);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(pollingResult);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.execute(new Environment(), false));

        assertNotNull(expectedException);
        assertEquals(expectedExceptionMessage, expectedException.getMessage());

        verify(mockExperiencePollingFailureResolver, times(ONCE)).getMessageForFailure(pollingResult);
    }

    @Test
    void testForceDeleteWitPollingErrorShouldNotThrow() {
        ImmutablePair<PollingResult, Exception> pollingResult = new ImmutablePair<>(PollingResult.FAILURE, null);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(pollingResult);

        underTest.execute(new Environment(), true);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testForceDeleteWitExperienceConnectorNonArgumentErrorShouldNotThrow() {
        doThrow(new IllegalStateException()).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        underTest.execute(new Environment(), true);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testNotForcedDeleteWitExperienceConnectorNonArgumentErrorShouldRethrow() {
        doThrow(new IllegalStateException()).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        assertThatThrownBy(() -> underTest.execute(new Environment(), false))
                .isExactlyInstanceOf(IllegalStateException.class);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testForceDeleteWitExperienceConnectorArgumentErrorShouldRethrow() {
        doThrow(new IllegalArgumentException()).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        assertThatThrownBy(() -> underTest.execute(new Environment(), true))
            .isExactlyInstanceOf(IllegalArgumentException.class);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }
}
