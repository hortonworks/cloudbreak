package com.sequenceiq.periscope.monitor.event;

public interface UpdateEvent {

    /**
     * Returns the id of the cluster which the update request
     * has been sent to.
     *
     * @return id of the cluster
     */
    String getClusterId();
}
