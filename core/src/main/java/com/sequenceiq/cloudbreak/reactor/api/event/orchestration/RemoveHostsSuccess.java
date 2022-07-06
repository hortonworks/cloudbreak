package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RemoveHostsSuccess extends StackEvent {

    private final Set<String> hostGroups;

    private final Set<String> hostNames;

    public RemoveHostsSuccess(Long stackId, Set<String> hostGroups, Set<String> hostNames) {
        super(stackId);
        this.hostGroups = hostGroups;
        this.hostNames = hostNames;
    }

    @JsonCreator
    public RemoveHostsSuccess(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroups") Set<String> hostGroups,
            @JsonProperty("hostNames") Set<String> hostNames) {
        super(selector, stackId);
        this.hostGroups = hostGroups;
        this.hostNames = hostNames;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
