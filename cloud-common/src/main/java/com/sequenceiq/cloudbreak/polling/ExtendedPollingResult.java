package com.sequenceiq.cloudbreak.polling;

import java.util.Set;

public class ExtendedPollingResult {

    private PollingResult pollingResult;

    private Exception exception;

    private Set<Long> failedInstanceIds;

    private Set<String> failedHostNames;

    private ExtendedPollingResult(PollingResult pollingResult, Exception exception, Set<Long> payload, Set<String> failedHostNames) {
        this.pollingResult = pollingResult;
        this.exception = exception;
        this.failedInstanceIds = payload;
        this.failedHostNames = failedHostNames;
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

    public Set<String> getFailedHostNames() {
        return failedHostNames;
    }

    public static class ExtendedPollingResultBuilder {
        private Set<Long> failedInstaceIds;

        private Set<String> failedHostNames;

        private Exception exception;

        private PollingResult pollingResult;

        public ExtendedPollingResultBuilder withPayload(Set<Long> failedInstanceIds) {
            this.failedInstaceIds = failedInstanceIds;
            return this;
        }

        public ExtendedPollingResultBuilder withPayloadWithHostNames(Set<String> failedHostNames) {
            this.failedHostNames = failedHostNames;
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
            return new ExtendedPollingResult(pollingResult, exception, failedInstaceIds, failedHostNames);
        }
    }

}
