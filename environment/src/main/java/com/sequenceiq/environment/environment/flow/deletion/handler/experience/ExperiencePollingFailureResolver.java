package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Component
public class ExperiencePollingFailureResolver {

    public String getMessageForFailure(ExtendedPollingResult result) {
        String additionalMessage = attemptToProvideMessageForPollingFailure(result.getPollingResult()).orElse("");
        return getIfNotNullOtherwise(result.getException(), Throwable::getMessage, additionalMessage);
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
