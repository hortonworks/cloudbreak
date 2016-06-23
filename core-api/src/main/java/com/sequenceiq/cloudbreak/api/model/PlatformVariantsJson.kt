package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PlatformVariantsJson : JsonEntity {

    var platformToVariants: Map<String, Collection<String>>? = null
    var defaultVariants: Map<String, String>? = null
}
