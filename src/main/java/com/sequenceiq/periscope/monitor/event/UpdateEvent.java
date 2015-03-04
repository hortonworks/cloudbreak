package com.sequenceiq.periscope.monitor.event;

public interface UpdateEvent extends TypedEvent {

    /**
     * Returns the id of the cluster which the update request
     * has been sent to.
     *
     * @return id of the cluster
     */
    long getClusterId();
}
