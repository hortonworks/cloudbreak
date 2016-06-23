package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AmbariCatalog : Versioned {

    @JsonProperty("ambari")
    var ambariInfo: AmbariInfo? = null

    override val version: String?
        get() {
            if (ambariInfo == null) {
                return null
            }
            return ambariInfo!!.version
        }

    override fun toString(): String {
        return "AmbariCatalog{version=$version'}"
    }
}
