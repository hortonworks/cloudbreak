package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResizeRecommendationV4Response implements JsonEntity {

    private final Set<String> scaleUpHostGroups;

    private final Set<String> scaleDownHostGroups;

    public ResizeRecommendationV4Response(Set<String> scaleUpHostGroups, Set<String> scaleDownHostGroups) {
        this.scaleUpHostGroups = scaleUpHostGroups;
        this.scaleDownHostGroups = scaleDownHostGroups;
    }

    public Set<String> getScaleUpHostGroups() {
        return scaleUpHostGroups;
    }

    public Set<String> getScaleDownHostGroups() {
        return scaleDownHostGroups;
    }
}
