package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class CloudbreakImageCatalog {

    @JsonProperty("cloudbreak")
    var ambariVersions: List<AmbariCatalog>? = null
}
