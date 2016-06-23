package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PlatformOrchestratorsJson : JsonEntity {

    var orchestrators: Map<String, Collection<String>>? = null
    var defaults: Map<String, String>? = null
}
