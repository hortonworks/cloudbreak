package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.function.Predicate;

public class OrchestratorStateRetryParams {
    private int maxRetry;

    private int maxRetryOnError = -1;

    private int sleepTime = -1;

    private Predicate<Exception> retryPredicate;

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public int getMaxRetryOnError() {
        return maxRetryOnError != -1 ? maxRetryOnError : maxRetry;
    }

    public void setMaxRetryOnError(int maxRetryOnError) {
        this.maxRetryOnError = maxRetryOnError;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Predicate<Exception> getRetryPredicate() {
        return retryPredicate;
    }

    public void setRetryPredicate(Predicate<Exception> retryPredicate) {
        this.retryPredicate = retryPredicate;
    }

    public boolean shouldRetry(Exception exception) {
        return retryPredicate == null || retryPredicate.test(exception);
    }
}
