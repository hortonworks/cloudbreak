package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class RegionsSpecification {
    @JsonProperty("items")
    var items: List<RegionSpecification>? = null
}
