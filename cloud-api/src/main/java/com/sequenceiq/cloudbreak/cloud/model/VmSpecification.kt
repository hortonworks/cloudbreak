package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class VmSpecification {
    @JsonProperty("value")
    var value: String? = null
    @JsonProperty("meta")
    var metaSpecification: MetaSpecification? = null
}
