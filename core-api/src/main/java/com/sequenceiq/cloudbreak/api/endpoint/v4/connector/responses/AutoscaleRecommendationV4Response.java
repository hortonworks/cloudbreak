package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoscaleRecommendationV4Response implements JsonEntity {

    private final Set<String> timeBasedHostGroups;

    private final Set<String> loadBasedHostGroups;

    public AutoscaleRecommendationV4Response() {
        this.timeBasedHostGroups = Set.of();
        this.loadBasedHostGroups = Set.of();
    }

    public AutoscaleRecommendationV4Response(Set<String> timeBasedHostGroups, Set<String> loadBasedHostGroups) {
        this.timeBasedHostGroups = Set.copyOf(timeBasedHostGroups);
        this.loadBasedHostGroups = Set.copyOf(loadBasedHostGroups);
    }

    public Set<String> getTimeBasedHostGroups() {
        return timeBasedHostGroups;
    }

    public Set<String> getLoadBasedHostGroups() {
        return loadBasedHostGroups;
    }
}
