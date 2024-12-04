package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayRecommendationV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> hostGroups;

    public GatewayRecommendationV4Response(Set<String> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

}
