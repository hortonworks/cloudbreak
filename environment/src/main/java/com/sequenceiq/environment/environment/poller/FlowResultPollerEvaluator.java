package com.sequenceiq.environment.environment.poller;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;

@Service
public class FlowResultPollerEvaluator {

    AttemptResult<Void> evaluateResult(List<AttemptResult<Void>> results) {
        return results.stream().anyMatch(it -> it.getState() == AttemptState.CONTINUE)
                ? AttemptResults.justContinue()
                : AttemptResults.justFinish();
    }
}
