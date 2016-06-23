package com.sequenceiq.cloudbreak.orchestrator.salt.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class RunningJobsResponse {

    @JsonProperty("return")
    var result: List<Map<String, Map<String, Any>>>? = null

    override fun toString(): String {
        return "RunningJobsResponse{"
        +"result=" + result
        +'}'
    }
}
