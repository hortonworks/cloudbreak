package com.sequenceiq.cloudbreak.polling;

import java.util.Set;

public class ExtendedPollingResult {

    private PollingResult pollingResult;

    private Exception exception;

    private Set<Long> failedInstanceIds;

    private ExtendedPollingResult(PollingResult pollingResult, Exception exception, Set<Long> payload) {
        this.pollingResult = pollingResult;
        this.exception = exception;
        this.failedInstanceIds = payload;
    }

    public boolean isSuccess() {
        return pollingResult.isSuccess();
    }

    public boolean isExited() {
        return pollingResult.isExited();
    }

    public boolean isTimeout() {
        return pollingResult.isTimeout();
    }

    public boolean isFailure() {
        return pollingResult.isFailure();
    }

    public PollingResult getPollingResult() {
        return pollingResult;
    }

    public Exception getException() {
        return exception;
    }

    public Set<Long> getFailedInstanceIds() {
        return failedInstanceIds;
    }

    public static class ExtendedPollingResultBuilder {
        private Set<Long> failedInstaceIds;

        private Exception exception;

        private PollingResult pollingResult;

        public ExtendedPollingResultBuilder withPayload(Set<Long> failedInstanceIds) {
            this.failedInstaceIds = failedInstanceIds;
            return this;
        }

        public ExtendedPollingResultBuilder withException(Exception e) {
            this.exception = e;
            return this;
        }

        public ExtendedPollingResultBuilder timeout() {
            this.pollingResult = PollingResult.TIMEOUT;
            return this;
        }

        public ExtendedPollingResultBuilder exit() {
            this.pollingResult = PollingResult.EXIT;
            return this;
        }

        public ExtendedPollingResultBuilder success() {
            this.pollingResult = PollingResult.SUCCESS;
            return this;
        }

        public ExtendedPollingResultBuilder failure() {
            this.pollingResult = PollingResult.FAILURE;
            return this;
        }

        public ExtendedPollingResultBuilder withPollingResult(PollingResult pollingResult) {
            this.pollingResult = pollingResult;
            return this;
        }

        public ExtendedPollingResult build() {
            return new ExtendedPollingResult(pollingResult, exception, failedInstaceIds);
        }
    }

}
