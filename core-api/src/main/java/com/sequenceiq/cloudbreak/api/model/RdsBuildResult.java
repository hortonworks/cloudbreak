package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsBuildResult")
public class RdsBuildResult implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.RESULT_DBS, required = true)
    private Map<String, String> results = new HashMap<>();

    public RdsBuildResult() {
    }

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}
