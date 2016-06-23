package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class PropertySpecification {

    @JsonProperty("Memory")
    var memory: String? = null
    @JsonProperty("Cpu")
    var cpu: String? = null
}
