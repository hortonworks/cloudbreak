package com.sequenceiq.cloudbreak.polling;

public enum PollingResult {
    TIMEOUT, EXIT, SUCCESS, FAILURE;

    public boolean isSuccess() {
        return equals(SUCCESS);
    }

    public boolean isExited() {
        return equals(EXIT);
    }

    public boolean isTimeout() {
        return equals(TIMEOUT);
    }

    public boolean isFailure() {
        return equals(FAILURE);
    }

}