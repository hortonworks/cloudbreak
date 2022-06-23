package com.sequenceiq.environment.environment.poller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;

class FlowResultPollerEvaluatorTest {

    private final FlowResultPollerEvaluator underTest = new FlowResultPollerEvaluator();

    @Test
    void testContinueAttemptStateExists() {
        List<AttemptResult<Void>> testlist = List.of(AttemptResults.justFinish(), AttemptResults.justFinish(), AttemptResults.justContinue());
        assertThat(underTest.evaluateResult(testlist)).usingRecursiveComparison().isEqualTo(AttemptResults.justContinue());
    }

    @Test
    void testContinueAttemptStateNotExists() {
        List<AttemptResult<Void>> testlist = List.of(AttemptResults.justFinish(), AttemptResults.justFinish(), AttemptResults.justFinish());
        assertThat(underTest.evaluateResult(testlist)).usingRecursiveComparison().isEqualTo(AttemptResults.justFinish());
    }
}
