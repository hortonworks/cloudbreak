package com.sequenceiq.cloudbreak.cloud.gcp.model

import com.fasterxml.jackson.annotation.JsonProperty

class ZoneDefinitionView {

    @JsonProperty("id")
    var id: String? = null
    @JsonProperty("kind")
    var kind: String? = null
    @JsonProperty("creationTimestamp")
    var creationTimestamp: String? = null
    @JsonProperty("name")
    var name: String? = null
    @JsonProperty("description")
    var description: String? = null
    @JsonProperty("status")
    var status: String? = null
    @JsonProperty("region")
    var region: String? = null
    @JsonProperty("selfLink")
    var selfLink: String? = null
}
