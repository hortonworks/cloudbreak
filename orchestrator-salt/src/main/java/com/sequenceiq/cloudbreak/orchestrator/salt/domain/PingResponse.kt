package com.sequenceiq.cloudbreak.orchestrator.salt.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class PingResponse {

    @JsonProperty("return")
    var result: List<Map<String, Boolean>>? = null
}
