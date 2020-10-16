package com.sequenceiq.cloudbreak.orchestrator.host;

public class OrchestratorStateRetryParams {
    private int maxRetry;

    private int maxRetryOnError = -1;

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
}
