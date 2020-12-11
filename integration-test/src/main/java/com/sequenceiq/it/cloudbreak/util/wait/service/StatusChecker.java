package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

public interface StatusChecker<T extends WaitObject> {

    boolean checkStatus(T t);

    void handleTimeout(T t);

    String successMessage(T t);

    boolean exitWaiting(T t);

    void handleException(Exception e);

    Map<String, String> getStatuses(T t);
}
