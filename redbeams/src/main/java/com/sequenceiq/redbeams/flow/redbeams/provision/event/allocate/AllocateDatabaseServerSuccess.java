package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

import java.util.List;

/**
 * The event that occurs when a database server has been allocated.
 */
public class AllocateDatabaseServerSuccess extends RedbeamsEvent {

    private final List<CloudResourceStatus> results;

    public AllocateDatabaseServerSuccess(Long resourceId, List<CloudResourceStatus> results) {
        super(resourceId);
        this.results = results;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerSuccess{"
                + "results=" + results
                + ", resourceId=" + getResourceId()
                + '}';
    }
}
