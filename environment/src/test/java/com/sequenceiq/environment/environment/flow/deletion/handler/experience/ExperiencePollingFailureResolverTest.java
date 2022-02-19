package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;

class ExperiencePollingFailureResolverTest {

    private static final String EXCEPTION_MSG = "Some interesting";

    private ExperiencePollingFailureResolver underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExperiencePollingFailureResolver();
    }

    @Test
    void testGetMessageForFailureWhenNoExceptionProvidedInPairAndPollingResultIsTimeoutThenTheExpectedMessageShouldCome() {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .timeout()
                .build();

        String result = underTest.getMessageForFailure(extendedPollingResult);

        assertNotNull(result);
        assertEquals("Timed out happened in the Experience deletion.", result);
    }

    @ParameterizedTest
    @EnumSource(
            value = PollingResult.class,
            names = "TIMEOUT",
            mode = EnumSource.Mode.EXCLUDE
    )
    void testGetMessageForFailureWhenNoExceptionProvidedInPairAndPollingResultIsNotTimeoutThenTheEmptyMessageShouldCome(PollingResult pollingResult) {
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .withPollingResult(pollingResult)
                .build();

        String result = underTest.getMessageForFailure(extendedPollingResult);

        assertNotNull(result);
        assertEquals("Other polling result: " + pollingResult, result);
    }

    @Test
    void testGetMessageForFailureWhenExceptionProvidedInPairAndPollingResultIsTimeoutThenTheMessageShouldComeFromTheException() {
        Exception exception = new Exception(EXCEPTION_MSG);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .timeout()
                .withException(exception)
                .build();

        String result = underTest.getMessageForFailure(extendedPollingResult);

        assertNotNull(result);
        assertEquals(exception.getMessage(), result);
    }

    @ParameterizedTest
    @EnumSource(
            value = PollingResult.class,
            names = "TIMEOUT",
            mode = EnumSource.Mode.EXCLUDE
    )
    void testGetMessageForFailureWhenExceptionProvidedInPairAndPollingResultIsNotTimeoutThenTheMessageShouldComeFromTheException(PollingResult pollingResult) {
        Exception exception = new Exception(EXCEPTION_MSG);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .withPollingResult(pollingResult)
                .withException(exception)
                .build();

        String result = underTest.getMessageForFailure(extendedPollingResult);

        assertNotNull(result);
        assertEquals(exception.getMessage(), result);
    }

}
