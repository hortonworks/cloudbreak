package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
open class TopologyBase : JsonEntity {
    var id: Long? = null
    var name: String? = null
    var description: String? = null
    var cloudPlatform: String? = null
    var endpoint: String? = null
    var nodes: Map<String, String>? = null
}
