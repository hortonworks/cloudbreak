package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class UpdateFailedEvent extends ApplicationEvent {

    private Exception causedBy;

    private long lastExceptionTimestamp;

    private boolean withMachineUser;

    private String pollingUserCrn;

    public UpdateFailedEvent(long clusterId) {
        super(clusterId);
    }

    public UpdateFailedEvent(Object source, Exception causedBy, long lastExceptionTimestamp, boolean withMachineUser, String userCrn) {
        super(source);
        this.causedBy = causedBy;
        this.lastExceptionTimestamp = lastExceptionTimestamp;
        this.withMachineUser = withMachineUser;
        this.pollingUserCrn = userCrn;
    }

    public long getClusterId() {
        return (long) source;
    }

    public Exception getCausedBy() {
        return causedBy;
    }

    public long getLastExceptionTimestamp() {
        return lastExceptionTimestamp;
    }

    public boolean isWithMachineUser() {
        return withMachineUser;
    }

    public String getPollingUserCrn() {
        return pollingUserCrn;
    }
}