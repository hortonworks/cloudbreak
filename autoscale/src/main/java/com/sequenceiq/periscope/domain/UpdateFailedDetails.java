package com.sequenceiq.periscope.domain;

public class UpdateFailedDetails {

    private Long lastExceptionTimestamp;

    private Long exceptionCount;

    private boolean withMachineUser;

    public UpdateFailedDetails() {
    }

    public UpdateFailedDetails(Long lastExceptionTimestamp, Long exceptionCount, boolean withMachineUser) {
        this.lastExceptionTimestamp = lastExceptionTimestamp;
        this.exceptionCount = exceptionCount;
        this.withMachineUser = withMachineUser;
    }

    public Long getLastExceptionTimestamp() {
        return lastExceptionTimestamp;
    }

    public void setLastExceptionTimestamp(Long lastExceptionTimestamp) {
        this.lastExceptionTimestamp = lastExceptionTimestamp;
    }

    public Long getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(Long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public boolean isWithMachineUser() {
        return withMachineUser;
    }

    public void setWithMachineUser(boolean withMachineUser) {
        this.withMachineUser = withMachineUser;
    }
}
