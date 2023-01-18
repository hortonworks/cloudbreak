package com.sequenceiq.mock.experience.response.common;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CpInternalEnvironmentResponse")
public class CpInternalEnvironmentResponse {

    private Set<CpInternalCluster> results;

    public Set<CpInternalCluster> getResults() {
        return results;
    }

    public void setResults(Set<CpInternalCluster> results) {
        this.results = results;
    }

}
