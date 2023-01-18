package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DatabaseV4BuildResponse implements JsonEntity {

    @Schema(description = ClusterModelDescription.RESULT_DBS, required = true)
    private Map<String, String> results = new HashMap<>();

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }
}
