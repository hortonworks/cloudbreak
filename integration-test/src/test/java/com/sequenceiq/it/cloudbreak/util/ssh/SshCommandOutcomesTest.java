package com.sequenceiq.it.cloudbreak.util.ssh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

class SshCommandOutcomesTest {

    private static final String INSTANCE_IP = "10.0.0.1";

    private static final String OTHER_INSTANCE_IP = "10.0.0.2";

    private static final String OPERATION = "Java version check";

    @Test
    void testConstructorRejectsNullList() {
        assertThrows(NullPointerException.class, () -> new SshCommandOutcomes(null));
    }

    @Test
    void testConstructorRejectsNullElement() {
        List<SshCommandOutcome> outcomes = new ArrayList<>();
        outcomes.add(null);

        assertThrows(NullPointerException.class, () -> new SshCommandOutcomes(outcomes));
    }

    @Test
    void testRequireAllExecutedReportsEveryUnreachableHost() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(
                SshCommandOutcome.unreachable(INSTANCE_IP, new SocketTimeoutException("Connect timed out")),
                SshCommandOutcome.authFailure(OTHER_INSTANCE_IP, new RuntimeException("Exhausted available authentication"))));

        TestFailException exception = assertThrows(TestFailException.class, underTest::requireAllExecuted);

        assertThat(exception.getMessage())
                .contains("2 instance(s)")
                .contains(INSTANCE_IP + "' (UNREACHABLE")
                .contains(OTHER_INSTANCE_IP + "' (AUTH_FAILURE");
    }

    @Test
    void testRequireAllExecutedWithOperationNamesTheOperation() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(SshCommandOutcome.unreachable(INSTANCE_IP, new RuntimeException("unreachable"))));

        TestFailException exception = assertThrows(TestFailException.class, () -> underTest.requireAllExecuted(OPERATION));

        assertThat(exception.getMessage()).startsWith(OPERATION + " could not be executed on 1 instance(s)");
    }

    @Test
    void testRequireAllExecutedIgnoresExitCodesAndReturnsSameInstance() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(SshCommandOutcome.executed(INSTANCE_IP, 1, "not found")));

        assertThat(underTest.requireAllExecuted()).isSameAs(underTest);
        assertThat(underTest.requireAllExecuted(OPERATION)).isSameAs(underTest);
    }

    @Test
    void testRequireAllSucceededReportsEveryFailingHost() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(
                SshCommandOutcome.executed(INSTANCE_IP, 1, "first failure"),
                SshCommandOutcome.executed(OTHER_INSTANCE_IP, 2, "second failure")));

        TestFailException exception = assertThrows(TestFailException.class, () -> underTest.requireAllSucceeded(OPERATION));

        assertThat(exception.getMessage())
                .contains(OPERATION + " failed on 2 instance(s)")
                .contains("first failure")
                .contains("second failure");
    }

    @Test
    void testRequireAllSucceededWithoutOperationUsesDefaultSubject() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(SshCommandOutcome.executed(INSTANCE_IP, 1, "failure")));

        TestFailException exception = assertThrows(TestFailException.class, underTest::requireAllSucceeded);

        assertThat(exception.getMessage()).startsWith("SSH command failed on 1 instance(s)");
    }

    @Test
    void testRequireAllSucceededReportsNotExecutedHostAsExecutionFailure() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(SshCommandOutcome.unreachable(INSTANCE_IP, new RuntimeException("unreachable"))));

        TestFailException exception = assertThrows(TestFailException.class, () -> underTest.requireAllSucceeded(OPERATION));

        assertThat(exception.getMessage()).startsWith(OPERATION + " could not be executed on 1 instance(s)");
    }

    @Test
    void testRequireAllSucceededPassesWhenEveryHostSucceededAndReturnsSameInstance() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(SshCommandOutcome.executed(INSTANCE_IP, 0, "ok")));

        assertThat(underTest.requireAllSucceeded()).isSameAs(underTest);
        assertThat(underTest.requireAllSucceeded(OPERATION)).isSameAs(underTest);
    }

    @Test
    void testToMapByInstanceIpPreservesOrder() {
        SshCommandOutcome first = SshCommandOutcome.executed(OTHER_INSTANCE_IP, 0, "first");
        SshCommandOutcome second = SshCommandOutcome.executed(INSTANCE_IP, 0, "second");
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(first, second));

        Map<String, SshCommandOutcome> result = underTest.toMapByInstanceIp();

        assertThat(result).containsExactly(Map.entry(OTHER_INSTANCE_IP, first), Map.entry(INSTANCE_IP, second));
    }

    @Test
    void testToMapByInstanceIpMapsValuesWithMapper() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(
                SshCommandOutcome.executed(INSTANCE_IP, 0, "first"),
                SshCommandOutcome.executed(OTHER_INSTANCE_IP, 0, "second")));

        Map<String, String> result = underTest.toMapByInstanceIp(SshCommandOutcome::commandOutput);

        assertThat(result).containsExactly(Map.entry(INSTANCE_IP, "first"), Map.entry(OTHER_INSTANCE_IP, "second"));
    }

    @Test
    void testToMapByInstanceIpAllowsNullMappedValue() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(SshCommandOutcome.executed(INSTANCE_IP, 0, "first")));

        Map<String, String> result = underTest.toMapByInstanceIp(outcome -> null);

        assertThat(result).containsOnlyKeys(INSTANCE_IP);
        assertThat(result.get(INSTANCE_IP)).isNull();
    }

    @Test
    void testToMapByInstanceIpWithFilterMapsOnlyMatchingOutcomes() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(
                SshCommandOutcome.executed(INSTANCE_IP, 1, "failed"),
                SshCommandOutcome.executed(OTHER_INSTANCE_IP, 0, "succeeded"),
                SshCommandOutcome.executed(INSTANCE_IP, 0, "succeeded again")));

        Map<String, String> result = underTest.toMapByInstanceIp(SshCommandOutcome::returnCodeSuccess, SshCommandOutcome::commandOutput);

        assertThat(result).containsExactly(Map.entry(OTHER_INSTANCE_IP, "succeeded"), Map.entry(INSTANCE_IP, "succeeded again"));
    }

    @Test
    void testToMapByInstanceIpRejectsDuplicateInstanceIp() {
        SshCommandOutcomes underTest = new SshCommandOutcomes(List.of(
                SshCommandOutcome.executed(INSTANCE_IP, 0, "first"),
                SshCommandOutcome.executed(INSTANCE_IP, 0, "second")));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, underTest::toMapByInstanceIp);

        assertThat(exception.getMessage()).contains("'" + INSTANCE_IP + "'");
    }
}
