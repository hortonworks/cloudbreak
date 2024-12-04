package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResizeRecommendationV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Set<String> scaleUpHostGroups;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Set<String> scaleDownHostGroups;

    public ResizeRecommendationV4Response(Set<String> scaleUpHostGroups, Set<String> scaleDownHostGroups) {
        this.scaleUpHostGroups = Set.copyOf(scaleUpHostGroups);
        this.scaleDownHostGroups = Set.copyOf(scaleDownHostGroups);
    }

    public Set<String> getScaleUpHostGroups() {
        return scaleUpHostGroups;
    }

    public Set<String> getScaleDownHostGroups() {
        return scaleDownHostGroups;
    }
}
