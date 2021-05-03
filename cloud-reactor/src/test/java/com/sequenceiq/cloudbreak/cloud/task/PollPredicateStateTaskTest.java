package com.sequenceiq.cloudbreak.cloud.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Objects;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@ExtendWith(MockitoExtension.class)
class PollPredicateStateTaskTest {

    private static final int RESULT = 12;

    @Mock
    private AuthenticatedContext authenticatedContext;

    private PollPredicateStateTask<Integer> underTest;

    @BeforeEach
    void setUp() {
        underTest = new DummyPollPredicateStateTask(authenticatedContext, false, Objects::nonNull);
    }

    static Object[][] constructorTestWhenNpeDataProvider() {
        return new Object[][]{
                // testCaseName authenticatedContext predicate
                {"null, null", null, null},
                {"null, predicate", null, (Predicate<? super Integer>) Objects::nonNull},
                {"authenticatedContext, null", mock(AuthenticatedContext.class), null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNpeDataProvider")
    void constructorTestWhenNpe(String testCaseName, AuthenticatedContext authenticatedContext, Predicate<? super Integer> predicate) {
        assertThrows(NullPointerException.class, () -> new DummyPollPredicateStateTask(authenticatedContext, false, predicate));
    }

    @Test
    void constructorTestWhenSuccess() {
        assertThat(underTest.getAuthenticatedContext()).isSameAs(authenticatedContext);
    }

    static Object[][] completedTestWhenSuccessDataProvider() {
        return new Object[][]{
                // testCaseName i resultExpected
                {"null, false", null, false},
                {"34, true", 34, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("completedTestWhenSuccessDataProvider")
    void completedTestWhenSuccess(String testCaseName, Integer i, boolean resultExpected) {
        assertThat(underTest.completed(i)).isEqualTo(resultExpected);
    }

    @Test
    void completedTestWhenException() {
        PollPredicateStateTask<Integer> underTest = new DummyPollPredicateStateTask(authenticatedContext, false,
                i -> {
                    throw new UnsupportedOperationException("Serious problem");
                });

        UnsupportedOperationException result = assertThrows(UnsupportedOperationException.class, () -> underTest.completed(null));

        assertThat(result).hasMessage("Serious problem");
    }

    @Test
    void doCallTest() {
        assertThat(underTest.doCall()).isEqualTo(RESULT);
    }

    private static class DummyPollPredicateStateTask extends PollPredicateStateTask<Integer> {

        private DummyPollPredicateStateTask(AuthenticatedContext authenticatedContext, boolean cancellable, Predicate<? super Integer> predicate) {
            super(authenticatedContext, cancellable, predicate);
        }

        @Override
        protected Integer doCall() {
            return RESULT;
        }

    }

}