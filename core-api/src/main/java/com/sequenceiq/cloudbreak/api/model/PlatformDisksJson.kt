package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PlatformDisksJson : JsonEntity {

    var diskTypes: Map<String, Collection<String>>? = null
    var defaultDisks: Map<String, String>? = null
    var diskMappings: Map<String, Map<String, String>>? = null

    init {
        this.diskTypes = HashMap<String, Collection<String>>()
        this.defaultDisks = HashMap<String, String>()
        this.diskMappings = HashMap<String, Map<String, String>>()
    }
}
