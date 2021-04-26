package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;

@Component
public class ExperiencePollingFailureResolver {

    public String getMessageForFailure(Pair<PollingResult, Exception> result) {
        String additionalMessage = attemptToProvideMessageForPollingFailure(result.getLeft()).orElse("");
        return getIfNotNullOtherwise(result.getRight(), Throwable::getMessage, additionalMessage);
    }

    private Optional<String> attemptToProvideMessageForPollingFailure(PollingResult pollingResult) {
        switch (pollingResult) {
            case TIMEOUT:
                return Optional.of("Timed out happened in the Experience deletion.");
            default:
                return Optional.of("Other polling result: " + pollingResult);
        }
    }

}
