package com.sequenceiq.it.cloudbreak.util.ssh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome.Result;

class SshCommandOutcomeTest {

    private static final String INSTANCE_IP = "10.0.0.1";

    private static final String OPERATION = "Java version check";

    @Test
    void testRequireSuccessPassesOnZeroExitCode() {
        SshCommandOutcome outcome = SshCommandOutcome.executed(INSTANCE_IP, 0, "ok");

        assertThat(outcome.requireSuccess(OPERATION)).isSameAs(outcome);
    }

    @Test
    void testRequireSuccessThrowsWithDetailsOnNonZeroExitCode() {
        SshCommandOutcome outcome = SshCommandOutcome.executed(INSTANCE_IP, 2, "something went wrong");

        TestFailException exception = assertThrows(TestFailException.class, () -> outcome.requireSuccess(OPERATION));

        assertThat(exception.getMessage())
                .contains(OPERATION)
                .contains("'" + INSTANCE_IP + "'")
                .contains("Exit code: 2")
                .contains("output: something went wrong");
    }

    @Test
    void testRequireSuccessThrowsOnMissingExitStatus() {
        SshCommandOutcome outcome = SshCommandOutcome.executed(INSTANCE_IP, SshCommandOutcome.MISSING_RETURN_CODE, "");

        TestFailException exception = assertThrows(TestFailException.class, () -> outcome.requireSuccess(OPERATION));

        assertThat(exception.getMessage()).contains(INSTANCE_IP).contains("Exit code: " + SshCommandOutcome.MISSING_RETURN_CODE);
    }

    @Test
    void testRequireSuccessReportsUnreachableHostAsSshFailure() {
        SocketTimeoutException cause = new SocketTimeoutException("Connect timed out");
        SshCommandOutcome outcome = SshCommandOutcome.unreachable(INSTANCE_IP, cause);

        TestFailException exception = assertThrows(TestFailException.class, () -> outcome.requireSuccess(OPERATION));

        assertThat(exception.getMessage())
                .contains(OPERATION + " could not be executed")
                .contains("UNREACHABLE")
                .contains("Connect timed out")
                .doesNotContain("Exit code");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void testRequireExecutedIgnoresExitCode() {
        SshCommandOutcome outcome = SshCommandOutcome.executed(INSTANCE_IP, 1, "not found");

        assertThat(outcome.requireExecuted()).isSameAs(outcome);
    }

    @Test
    void testRequireExecutedWithOperationNamesTheOperation() {
        SshCommandOutcome outcome = SshCommandOutcome.authFailure(INSTANCE_IP, new RuntimeException("Exhausted available authentication"));

        TestFailException exception = assertThrows(TestFailException.class, () -> outcome.requireExecuted(OPERATION));

        assertThat(exception.getMessage()).startsWith(OPERATION + " could not be executed on '" + INSTANCE_IP + "'").contains("AUTH_FAILURE");
    }

    @Test
    void testNotExecutedCommandHasNoOutputAndMissingReturnCode() {
        SshCommandOutcome outcome = SshCommandOutcome.unreachable(INSTANCE_IP, new SocketTimeoutException("Connect timed out"));

        assertThat(outcome.commandOutput()).isNull();
        assertThat(outcome.returnCode()).isEqualTo(SshCommandOutcome.MISSING_RETURN_CODE);
        assertThat(outcome.returnCodeSuccess()).isFalse();
    }

    @Test
    void testToStringOfNotExecutedCommandDoesNotThrow() {
        SshCommandOutcome outcome = SshCommandOutcome.timedOut(INSTANCE_IP, new SocketTimeoutException("Timeout of 600000ms while waiting for data"));

        assertThat(outcome.toString()).contains(INSTANCE_IP).contains("TIMED_OUT");
    }

    @Test
    void testExecutedCommandWithoutOutputHasEmptyOutput() {
        assertThat(SshCommandOutcome.executed(INSTANCE_IP, 0, "").commandOutput()).isEmpty();
    }

    @Test
    void testExecutedCommandWithNullOutputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SshCommandOutcome.executed(INSTANCE_IP, 0, null));
    }

    @Test
    void testExecutedCommandWithInvalidNegativeReturnCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SshCommandOutcome.executed(INSTANCE_IP, -2, "output"));
    }

    @Test
    void testBlankInstanceIpIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SshCommandOutcome.executed(" ", 0, "output"));
    }

    @Test
    void testNullResultIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SshCommandOutcome(INSTANCE_IP, null, 0, "output", null));
    }

    @Test
    void testExecutedCommandWithThrowableIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SshCommandOutcome(INSTANCE_IP, Result.EXECUTED, 0, "output", new RuntimeException("unexpected")));
    }

    @Test
    void testNotExecutedCommandWithOutputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SshCommandOutcome(INSTANCE_IP, Result.UNREACHABLE,
                SshCommandOutcome.MISSING_RETURN_CODE, "output", new SocketTimeoutException("Connect timed out")));
    }

    @Test
    void testNotExecutedCommandWithoutThrowableIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SshCommandOutcome(INSTANCE_IP, Result.TIMED_OUT,
                SshCommandOutcome.MISSING_RETURN_CODE, null, null));
    }

    @Test
    void testNotExecutedCommandWithReturnCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SshCommandOutcome(INSTANCE_IP, Result.AUTH_FAILURE, 0, null,
                new RuntimeException("Exhausted available authentication")));
    }
}
