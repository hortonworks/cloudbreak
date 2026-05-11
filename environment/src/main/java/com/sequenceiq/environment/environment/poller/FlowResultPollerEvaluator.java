package com.sequenceiq.environment.environment.poller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class FlowResultPollerEvaluator {

    AttemptResult<Void> evaluateResult(List<AttemptResult<Void>> results) {
        return results.stream().anyMatch(it -> it.getState() == AttemptState.CONTINUE)
                ? AttemptResults.justContinue()
                : AttemptResults.justFinish();
    }

    public AttemptResult<List<FlowIdentifier>> attemptResultFinisher(List<AttemptResult<FlowIdentifier>> attemptResultList) {
        List<AttemptResult<FlowIdentifier>> anyFailure =
                attemptResultList.stream().filter(it -> it.getState() == AttemptState.BREAK).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(anyFailure)) {
            String errorMessages = anyFailure.stream()
                    .map(AttemptResult::getMessage)
                    .collect(Collectors.joining(", "));
            return AttemptResults.breakFor(errorMessages);
        }

        boolean shouldContinue = attemptResultList.stream().anyMatch(result -> result.getState() == AttemptState.CONTINUE);
        if (shouldContinue) {
            return AttemptResults.justContinue();
        }

        List<FlowIdentifier> successValues = attemptResultList.stream()
                .map(AttemptResult::getResult)
                .collect(Collectors.toList());

        return AttemptResults.finishWith(successValues);
    }
}
