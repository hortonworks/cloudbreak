package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class UpdateFailedEvent extends ApplicationEvent {

    private Exception causedBy;

    public UpdateFailedEvent(long clusterId) {
        super(clusterId);
    }

    public UpdateFailedEvent(long clusterId, Exception causedBy) {
        super(clusterId);
        this.causedBy = causedBy;
    }

    public long getClusterId() {
        return (long) source;
    }

    public Exception getCausedBy() {
        return causedBy;
    }
}