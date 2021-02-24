package com.sequenceiq.environment.experience.common.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpInternalEnvironmentResponse {

    private Set<CpInternalCluster> results;

    public Set<CpInternalCluster> getResults() {
        return results;
    }

    public void setResults(Set<CpInternalCluster> results) {
        this.results = results;
    }

}
