package com.sequenceiq.cloudbreak.service;

public interface StatusCheckerTask<T> {

    boolean checkStatus(T t);

    void handleTimeout(T t);

    String successMessage(T t);

    boolean exitPolling(T t);
}
