package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class ConfigsSpecification {
    @JsonProperty("configs")
    var configs: List<ConfigSpecification>? = null
    @JsonProperty("properties")
    var properties: PropertySpecification? = null
}
