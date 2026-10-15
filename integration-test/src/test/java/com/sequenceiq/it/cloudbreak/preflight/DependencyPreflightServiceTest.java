package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@ExtendWith(MockitoExtension.class)
class DependencyPreflightServiceTest {

    @Mock
    private PreflightCheck passingCheck;

    @Mock
    private PreflightCheck failingCheck;

    @Mock
    private PreflightCheck secondFailingCheck;

    @Mock
    private PreflightCheck skippedCheck;

    @InjectMocks
    private DependencyPreflightService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "enabled", true);
    }

    @Test
    void testRunPreflightChecksOrFailDoesNotThrowWhenAllPass() {
        when(passingCheck.check()).thenReturn(PreflightCheckResult.pass("passing", "ok"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(passingCheck));

        assertThatCode(underTest::runPreflightChecksOrFail).doesNotThrowAnyException();
    }

    @Test
    void testRunPreflightChecksOrFailDoesNotThrowWhenAllSkipped() {
        when(skippedCheck.check()).thenReturn(PreflightCheckResult.skip("skipped", "not applicable"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(skippedCheck));

        assertThatCode(underTest::runPreflightChecksOrFail).doesNotThrowAnyException();
    }

    @Test
    void testRunPreflightChecksOrFailThrowsWithAggregatedMessageWhenAnyFails() {
        when(passingCheck.check()).thenReturn(PreflightCheckResult.pass("passing", "ok"));
        when(failingCheck.check()).thenReturn(PreflightCheckResult.fail("failing", "unreachable"));
        when(secondFailingCheck.check()).thenReturn(PreflightCheckResult.fail("second-failing", "quota exceeded"));
        when(skippedCheck.check()).thenReturn(PreflightCheckResult.skip("skipped", "not applicable"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(passingCheck, failingCheck, skippedCheck, secondFailingCheck));

        assertThatThrownBy(underTest::runPreflightChecksOrFail)
                .isInstanceOf(TestFailException.class)
                .hasMessageContaining("failing: unreachable")
                .hasMessageContaining("second-failing: quota exceeded")
                .hasMessageNotContaining("passing: ok")
                .hasMessageNotContaining("skipped: not applicable");
    }

    @Test
    void testRunPreflightChecksOrFailRunsEachCheckBeforeLaterCheckThrows() {
        when(passingCheck.check()).thenReturn(PreflightCheckResult.pass("passing", "ok"));
        when(failingCheck.check()).thenThrow(new IllegalStateException("sdk error"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(passingCheck, failingCheck, secondFailingCheck));

        assertThatThrownBy(underTest::runPreflightChecksOrFail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("sdk error");
        verify(passingCheck).check();
        verify(secondFailingCheck, never()).check();
    }

    @Test
    void testRunPreflightChecksOrFailDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(underTest, "enabled", false);
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(failingCheck));

        assertThatCode(underTest::runPreflightChecksOrFail).doesNotThrowAnyException();
        verify(failingCheck, never()).check();
    }
}
