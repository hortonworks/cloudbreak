package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependentHostGroupsV4Response {

    @ApiModelProperty
    private Map<String, Set<String>> dependentHostGroups;

    public Map<String, Set<String>> getDependentHostGroups() {
        return dependentHostGroups;
    }

    public void setDependentHostGroups(Map<String, Set<String>> dependentHostGroups) {
        this.dependentHostGroups = dependentHostGroups;
    }
}
