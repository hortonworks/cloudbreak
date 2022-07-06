package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when a database server has been terminated.
 */
public class TerminateDatabaseServerSuccess extends RedbeamsEvent {

    private final List<CloudResourceStatus> results;

    @JsonCreator
    public TerminateDatabaseServerSuccess(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("results") List<CloudResourceStatus> results) {
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
