package com.sequenceiq.cloudbreak.polling;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public interface StatusCheckerTask<T> {

    boolean checkStatus(T t);

    void handleTimeout(T t);

    String successMessage(T t);

    boolean exitPolling(T t);

    void handleException(Exception e);

    default Set<Long> getFailedInstancePrivateIds() {
        return Collections.emptySet();
    }

    default boolean initialExitCheck(T t) {
        return true;
    }

    default void sendFailureEvent(T t) {

    }

    default void sendTimeoutEvent(T t) {

    }

    default void sendWarningTimeoutEventIfNecessary(T t) {
    }

    default Optional<String> additionalTimeoutErrorMessage() {
        return Optional.empty();
    }

    default long increasePollingBackoff(long defaultInterval, long consecutiveFailures) {
        return defaultInterval;
    }
}
