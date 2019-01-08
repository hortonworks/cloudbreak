package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class BlueprintV4ViewResponses {

    private Set<BlueprintV4ViewResponse> blueprints = new HashSet<>();

    public Set<BlueprintV4ViewResponse> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(Set<BlueprintV4ViewResponse> blueprints) {
        this.blueprints = blueprints;
    }

    public static final BlueprintV4ViewResponses blueprintV4ViewResponses(Set<BlueprintV4ViewResponse> blueprints) {
        BlueprintV4ViewResponses blueprintV4ViewResponses = new BlueprintV4ViewResponses();
        blueprintV4ViewResponses.setBlueprints(blueprints);
        return blueprintV4ViewResponses;
    }
}
