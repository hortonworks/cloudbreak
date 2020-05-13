package com.sequenceiq.environment.environment.experience.dwx.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "CpInternalEnvironmentResponse")
public class CpInternalEnvironmentResponse {

    private Set<CpInternalCluster> results;

    public Set<CpInternalCluster> getResults() {
        return results;
    }

    public void setResults(Set<CpInternalCluster> results) {
        this.results = results;
    }

}
