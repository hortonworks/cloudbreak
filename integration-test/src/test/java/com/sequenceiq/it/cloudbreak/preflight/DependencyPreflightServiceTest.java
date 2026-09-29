package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DependencyPreflightServiceTest {

    private PreflightCheck passingCheck;

    private PreflightCheck failingCheck;

    private PreflightCheck secondFailingCheck;

    private PreflightCheck skippedCheck;

    private DependencyPreflightService underTest;

    @BeforeMethod
    public void setUp() {
        passingCheck = mock(PreflightCheck.class);
        failingCheck = mock(PreflightCheck.class);
        secondFailingCheck = mock(PreflightCheck.class);
        skippedCheck = mock(PreflightCheck.class);

        underTest = new DependencyPreflightService();
        ReflectionTestUtils.setField(underTest, "enabled", true);
    }

    @Test
    public void testRunPreflightChecksOrFailDoesNotThrowWhenAllPass() {
        when(passingCheck.check()).thenReturn(PreflightCheckResult.pass("passing", "ok"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(passingCheck));

        assertThatCode(underTest::runPreflightChecksOrFail).doesNotThrowAnyException();
    }

    @Test
    public void testRunPreflightChecksOrFailDoesNotThrowWhenAllSkipped() {
        when(skippedCheck.check()).thenReturn(PreflightCheckResult.skip("skipped", "not applicable"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(skippedCheck));

        assertThatCode(underTest::runPreflightChecksOrFail).doesNotThrowAnyException();
    }

    @Test
    public void testRunPreflightChecksOrFailThrowsWithAggregatedMessageWhenAnyFails() {
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
    public void testRunPreflightChecksOrFailRunsEachCheckBeforeLaterCheckThrows() {
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
    public void testRunPreflightChecksOrFailDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(underTest, "enabled", false);
        when(failingCheck.check()).thenReturn(PreflightCheckResult.fail("failing", "unreachable"));
        ReflectionTestUtils.setField(underTest, "preflightChecks", List.of(failingCheck));

        assertThatCode(underTest::runPreflightChecksOrFail).doesNotThrowAnyException();
        verify(failingCheck, never()).check();
    }
}
