package com.sequenceiq.cloudbreak.orchestrator.salt.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class ApplyResponse {

    @JsonProperty("return")
    var result: List<Map<String, Any>>? = null

    val jid: String
        get() = result!![0]["jid"].toString()

}
