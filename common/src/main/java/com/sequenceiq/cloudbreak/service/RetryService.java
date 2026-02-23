package com.sequenceiq.cloudbreak.service;

import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service("DefaultRetryService")
public class RetryService implements Retry {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);

    @Inject
    private Optional<RetryErrorPatterns> retryErrorPatterns;

    @Override
    @Retryable(retryFor = ActionFailedException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public void testWith2SecDelayMax5Times(Runnable action) throws ActionFailedException {
        action.run();
    }

    @Override
    @Retryable(retryFor = ActionFailedException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public <T> T testWith2SecDelayMax5Times(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    @Retryable(
            retryFor = ActionFailedException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    public <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    @Retryable(
            retryFor = ActionFailedException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public <T> T testWith1SecDelayMax5Times(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    @Retryable(retryFor = ActionFailedException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 5, maxDelay = 300000))
    public <T> T testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    @Retryable(
            retryFor = ActionFailedException.class,
            backoff = @Backoff(delay = 1000, multiplier = 1, maxDelay = 10000)
    )
    public <T> T testWith1SecDelayMax3Times(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    public <T> T testWithoutRetry(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    @Retryable(
            retryFor = ActionFailedException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public <T> T testWith1SecDelayMax5TimesAndMultiplier2WithCheckRetriable(Supplier<T> action) throws ActionFailedException {
        return runWithCheckRetriable(action);
    }

    private <T> T runWithCheckRetriable(Supplier<T> action) throws ActionFailedException {
        try {
            return action.get();
        } catch (ActionFailedException e) {
            throw isRetriable(e) ? e : new ActionFailedNonRetryableException(e.getCause());
        }
    }

    private boolean isRetriable(ActionFailedException e) {
        StringBuilder messageBuilder = new StringBuilder()
                .append(e.getMessage());
        if (e.getCause() != null) {
            messageBuilder.append(". ")
                    .append(ExceptionUtils.getRootCauseMessage(e.getCause()));
            String message = messageBuilder.toString();
            boolean retriable = retryErrorPatterns.isEmpty() || !retryErrorPatterns.get().containsNonRetryableError(message);
            LOGGER.debug("Checked if message '{}' is retriable and the result is {}", message, retriable);
            return retriable;
        } else {
            LOGGER.debug("Empty cause, assuming it is retryable");
            return true;
        }
    }
}
