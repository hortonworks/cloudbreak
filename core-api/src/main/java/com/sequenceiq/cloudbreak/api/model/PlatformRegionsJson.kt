package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PlatformRegionsJson : JsonEntity {

    var regions: Map<String, Collection<String>>? = null
    var availabilityZones: Map<String, Map<String, Collection<String>>>? = null
    var defaultRegions: Map<String, String>? = null

    init {
        this.regions = HashMap<String, Collection<String>>()
        this.availabilityZones = HashMap<String, Map<String, Collection<String>>>()
        this.defaultRegions = HashMap<String, String>()
    }
}
