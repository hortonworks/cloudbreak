package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

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

    private EnvironmentExperienceDeletionAction underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(new ImmutablePair<>(PollingResult.SUCCESS, null));
        underTest = new EnvironmentExperienceDeletionAction(mockExperienceConnectorService, mockExperiencePollingService, mockExperiencePollingFailureResolver);
    }

    @Test
    void testExecuteShouldInvokeDeleteConnectedExperiences() {
        underTest.execute(new Environment());

        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any());
        verify(mockExperienceConnectorService, times(ONCE)).deleteConnectedExperiences(any(EnvironmentExperienceDto.class));
    }

    @Test
    void testExecuteWhenPollingResultWasNotSuccessfulButNoExceptionHasGivenInThePollingResultPairThenExperienceOperationFailedExceptionShouldCome() {
        ImmutablePair<PollingResult, Exception> pollingResult = new ImmutablePair<>(PollingResult.EXIT, null);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(pollingResult);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.execute(new Environment()));

        assertNotNull(expectedException);
        assertEquals(FAILURE_BASIC_MSG, expectedException.getMessage());

        verify(mockExperiencePollingFailureResolver, never()).getMessageForFailure(any());
    }

    @Test
    void testExecuteWhenPollingResultWasNotSuccessfulAndExceptionHasGivenInThePollingResultPairThenExperienceOperationFailedExceptionShouldCome() {
        String resolvedMessage = "Because of reasons...";
        String expectedExceptionMessage = FAILURE_BASIC_MSG + " " + resolvedMessage;
        ImmutablePair<PollingResult, Exception> pollingResult = new ImmutablePair<>(PollingResult.EXIT, new Exception());
        when(mockExperiencePollingFailureResolver.getMessageForFailure(pollingResult)).thenReturn(resolvedMessage);
        when(mockExperiencePollingService.pollWithTimeout(
                any(ExperienceDeletionRetrievalTask.class),
                any(ExperiencePollerObject.class),
                eq(Long.valueOf(EXPERIENCE_RETRYING_INTERVAL)),
                eq(EXPERIENCE_RETRYING_COUNT),
                eq(1)))
                .thenReturn(pollingResult);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.execute(new Environment()));

        assertNotNull(expectedException);
        assertEquals(expectedExceptionMessage, expectedException.getMessage());

        verify(mockExperiencePollingFailureResolver, times(ONCE)).getMessageForFailure(pollingResult);
    }

}
