package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

@ExtendWith(MockitoExtension.class)
class EnvironmentExperienceDeletionActionTest {

    private static final String FAILURE_BASIC_MSG = "Failed to delete Experience!";

    private static final String GENERIC_TEST_EXCEPTION_MESSAGE = "something bad";

    private static final boolean NO_FORCE_DELETE = false;

    private static final boolean FORCE_DELETE = true;

    private static final int ONCE = 1;

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
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);
        underTest.execute(new Environment(), NO_FORCE_DELETE);

        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any());
        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testExecuteWhenPollingResultWasNotSuccessfulButNoExceptionHasGivenInThePollingResultPairThenExperienceOperationFailedExceptionShouldCome() {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .build();
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.execute(new Environment(), NO_FORCE_DELETE));

        assertNotNull(expectedException);
        assertEquals(FAILURE_BASIC_MSG, expectedException.getMessage());

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testExecuteWhenPollingResultWasNotSuccessfulAndExceptionHasGivenInThePollingResultPairThenExperienceOperationFailedExceptionShouldCome() {
        String resolvedMessage = "Because of reasons...";
        String expectedExceptionMessage = FAILURE_BASIC_MSG + " " + resolvedMessage;
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .withException(new Exception())
                .build();
        when(mockExperiencePollingFailureResolver.getMessageForFailure(extendedPollingResult)).thenReturn(resolvedMessage);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.execute(new Environment(), NO_FORCE_DELETE));

        assertNotNull(expectedException);
        assertEquals(expectedExceptionMessage, expectedException.getMessage());

        verify(mockExperiencePollingFailureResolver, times(ONCE)).getMessageForFailure(extendedPollingResult);
    }

    @Test
    void testExecuteWhenPollingResultWasGotBackWithExitThenReCheckShouldHappen() {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .exit()
                .build();
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);

        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class))).thenReturn(0);

        underTest.execute(new Environment(), NO_FORCE_DELETE);

        verify(mockExperienceConnectorService, times(ONCE)).getConnectedExperienceCount(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testExecuteWhenPollingResultWasGotBackWithExitAndTheConnectedExperienceCountIsMoreThanZeroThenReCheckShouldHappenAndFailureShouldHappen() {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .exit()
                .build();
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);

        when(mockExperienceConnectorService.getConnectedExperienceCount(any(EnvironmentExperienceDto.class))).thenReturn(1);

        assertThrows(ExperienceOperationFailedException.class, () -> underTest.execute(new Environment(), NO_FORCE_DELETE));

        verify(mockExperienceConnectorService, times(ONCE)).getConnectedExperienceCount(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testExecuteWhenPollingResultWasGotBackWithOtherThanExitThenReCheckShouldNotHappen() {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .build();
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);

        assertThrows(ExperienceOperationFailedException.class, () -> underTest.execute(new Environment(), NO_FORCE_DELETE));

        verify(mockExperienceConnectorService, never()).getConnectedExperienceCount(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testForceDeleteWitPollingErrorShouldNotThrow() {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .build();
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL_IN_MILLISECONDS)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(extendedPollingResult);

        underTest.execute(new Environment(), FORCE_DELETE);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testForceDeleteWitExperienceConnectorNonArgumentErrorShouldNotThrow() {
        doThrow(new IllegalStateException()).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        underTest.execute(new Environment(), FORCE_DELETE);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testNotForcedDeleteWitExperienceConnectorNonArgumentErrorShouldRethrow() {
        doThrow(new IllegalStateException()).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        assertThatThrownBy(() -> underTest.execute(new Environment(), NO_FORCE_DELETE))
                .isExactlyInstanceOf(ExperienceOperationFailedException.class);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testForceDeleteWitExperienceConnectorArgumentErrorShouldRethrow() {
        doThrow(new IllegalArgumentException()).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        assertThatThrownBy(() -> underTest.execute(new Environment(), FORCE_DELETE))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testWhenDeletionThrowsRuntimeExceptionOtherThanIllegalStateAndArgumentExceptionWithoutForceDeleteThenItShouldBeRethrown() {
        RuntimeException expectedException = new RuntimeException(GENERIC_TEST_EXCEPTION_MESSAGE);
        doThrow(expectedException).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        RuntimeException resultException = assertThrows(RuntimeException.class, () -> underTest.execute(new Environment(), NO_FORCE_DELETE));

        assertEquals(expectedException, resultException);
        assertEquals(expectedException.getMessage(), resultException.getMessage());

        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testWhenDeletionThrowsRuntimeExceptionOtherThanIllegalStateAndArgumentExceptionWithForceDeleteFalseShouldReturn() {
        doThrow(new RuntimeException(GENERIC_TEST_EXCEPTION_MESSAGE)).when(mockExperienceConnectorService).deleteConnectedExperiences(any());

        underTest.execute(new Environment(), FORCE_DELETE);

        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any(EnvironmentExperienceDto.class));
        verify(mockExperiencePollingService, never())
                .pollWithTimeout(
                        any(ExperienceDeletionRetrievalTask.class),
                        any(ExperiencePollerObject.class),
                        anyLong(),
                        anyInt(),
                        anyInt()
                );
    }

}
