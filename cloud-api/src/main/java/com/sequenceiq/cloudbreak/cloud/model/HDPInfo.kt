package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class HDPInfo : Versioned {

    override var version: String? = null
    var repo: HDPRepo? = null
    var images: Map<String, Map<String, String>>? = null

    override fun toString(): String {
        return "HDPInfo{version='$version'}"
    }
}
