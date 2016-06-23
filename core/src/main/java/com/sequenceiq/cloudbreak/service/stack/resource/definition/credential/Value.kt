package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential

import com.fasterxml.jackson.annotation.JsonProperty

class Value {

    @JsonProperty("name")
    var name: String? = null
    @JsonProperty("type")
    var type: String? = null
    @JsonProperty("encrypted")
    var encrypted: Boolean? = null
    @JsonProperty("optional")
    var optional: Boolean? = null
}
