package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class VmsSpecification {
    @JsonProperty("items")
    var items: List<VmSpecification>? = null
}
