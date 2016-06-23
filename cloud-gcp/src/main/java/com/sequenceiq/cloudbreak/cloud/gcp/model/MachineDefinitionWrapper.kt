package com.sequenceiq.cloudbreak.cloud.gcp.model

import com.fasterxml.jackson.annotation.JsonProperty

class MachineDefinitionWrapper {

    @JsonProperty("id")
    var id: String? = null
    @JsonProperty("kind")
    var kind: String? = null
    @JsonProperty("selfLink")
    var selfLink: String? = null
    @JsonProperty("items")
    var items: Map<String, Any>? = null
}
