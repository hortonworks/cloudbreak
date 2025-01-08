package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependentHostGroupsV4Response {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Set<String>> dependentHostGroups = new HashMap<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Set<String>> dependentComponents = new HashMap<>();

    public Map<String, Set<String>> getDependentHostGroups() {
        return dependentHostGroups;
    }

    public void setDependentHostGroups(Map<String, Set<String>> dependentHostGroups) {
        this.dependentHostGroups = dependentHostGroups;
    }

    public Map<String, Set<String>> getDependentComponents() {
        return dependentComponents;
    }

    public void setDependentComponents(Map<String, Set<String>> dependentComponents) {
        this.dependentComponents = dependentComponents;
    }
}
