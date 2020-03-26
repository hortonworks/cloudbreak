package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DatabaseV4BuildResponse implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.RESULT_DBS, required = true)
    private Map<String, String> results = new HashMap<>();

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}
