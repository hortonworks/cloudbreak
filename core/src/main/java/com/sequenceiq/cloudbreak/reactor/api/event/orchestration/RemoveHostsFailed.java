package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RemoveHostsFailed extends StackFailureEvent {

    private final Set<String> hostGroups;

    private final Set<String> failedHostNames;

    public RemoveHostsFailed(Long stackId, Exception exception, Set<String> hostGroups, Set<String> failedHostNames) {
        super(stackId, exception);
        this.failedHostNames = failedHostNames;
        this.hostGroups = hostGroups;
    }

    @JsonCreator
    public RemoveHostsFailed(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("hostGroups") Set<String> hostGroups,
            @JsonProperty("failedHostNames") Set<String> failedHostNames) {
        super(selector, stackId, exception);
        this.failedHostNames = failedHostNames;
        this.hostGroups = hostGroups;
    }

    public Set<String> getFailedHostNames() {
        return failedHostNames;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

}
