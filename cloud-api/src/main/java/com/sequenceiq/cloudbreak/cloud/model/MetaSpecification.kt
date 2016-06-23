package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class MetaSpecification {
    @JsonProperty("configs")
    var configSpecification: List<ConfigSpecification>? = null
    @JsonProperty("properties")
    var properties: PropertySpecification? = null
}
