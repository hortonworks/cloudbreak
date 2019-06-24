package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

import java.util.List;

/**
 * The event that occurs when a database server has been terminated.
 */
public class TerminateDatabaseServerSuccess extends TerminateDatabaseServerResponse {

    private final List<CloudResourceStatus> results;

    public TerminateDatabaseServerSuccess(Long resourceId, List<CloudResourceStatus> results) {
        super(resourceId);
        this.results = results;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "TerminateDatabaseServerSuccess{"
                + "results=" + results
                + ", resourceId=" + getResourceId()
                + '}';
    }
}
