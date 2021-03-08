package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

public interface StatusChecker<T extends WaitObject> {

    boolean checkStatus(T waitObject);

    void handleTimeout(T waitObject);

    String successMessage(T waitObject);

    boolean exitWaiting(T waitObject);

    void handleException(Exception e);

    Map<String, String> getStatuses(T waitObject);

    void refresh(T waitObject);
}
