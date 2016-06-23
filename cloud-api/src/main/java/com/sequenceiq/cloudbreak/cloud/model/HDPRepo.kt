package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class HDPRepo {

    var stack: Map<String, String>? = null
    var util: Map<String, String>? = null
}
