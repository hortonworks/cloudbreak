package com.sequenceiq.environment.environment.poller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;

@Component
public class ClusterPollerResultEvaluator {

    AttemptResult<Void> evaluateResult(List<AttemptResult<Void>> results) {
        Optional<AttemptResult<Void>> error = results.stream().filter(it -> it.getState() == AttemptState.BREAK).findFirst();
        if (error.isPresent()) {
            return error.get();
        }
        long count = results.stream().filter(it -> it.getState() == AttemptState.CONTINUE).count();
        if (count > 0) {
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }
}
