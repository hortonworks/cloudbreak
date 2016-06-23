package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class RegionSpecification {
    @JsonProperty("name")
    var name: String? = null
    @JsonProperty("zones")
    var zones: List<String>? = null
}
