package com.sequenceiq.cloudbreak.polling;

import java.util.Collections;
import java.util.Set;

public interface StatusCheckerTask<T> {

    boolean checkStatus(T t);

    void handleTimeout(T t);

    String successMessage(T t);

    boolean exitPolling(T t);

    void handleException(Exception e);

    default Set<Long> getFailedInstanceIds() {
        return Collections.emptySet();
    }

    default boolean initialExitCheck(T t) {
        return true;
    }

    default void sendFailureEvent(T t) {

    }

    default void sendTimeoutEvent(T t) {

    }
}
