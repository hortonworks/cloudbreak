package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
        Pair<PollingResult, Exception> pollingResultPair = new ImmutablePair<>(PollingResult.TIMEOUT, null);

        String result = underTest.getMessageForFailure(pollingResultPair);

        assertNotNull(result);
        assertEquals("Timed out.", result);
    }

    @ParameterizedTest
    @EnumSource(
            value = PollingResult.class,
            names = "TIMEOUT",
            mode = EnumSource.Mode.EXCLUDE
    )
    void testGetMessageForFailureWhenNoExceptionProvidedInPairAndPollingResultIsNotTimeoutThenTheEmptyMessageShouldCome(PollingResult pollingResult) {
        Pair<PollingResult, Exception> pollingResultPair = new ImmutablePair<>(pollingResult, null);

        String result = underTest.getMessageForFailure(pollingResultPair);

        assertNotNull(result);
        assertTrue(StringUtils.isEmpty(result));
    }

    @Test
    void testGetMessageForFailureWhenExceptionProvidedInPairAndPollingResultIsTimeoutThenTheMessageShouldComeFromTheException() {
        Exception exception = new Exception(EXCEPTION_MSG);
        Pair<PollingResult, Exception> pollingResultPair = new ImmutablePair<>(PollingResult.TIMEOUT, exception);

        String result = underTest.getMessageForFailure(pollingResultPair);

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
        Pair<PollingResult, Exception> pollingResultPair = new ImmutablePair<>(pollingResult, exception);

        String result = underTest.getMessageForFailure(pollingResultPair);

        assertNotNull(result);
        assertEquals(exception.getMessage(), result);
    }

}