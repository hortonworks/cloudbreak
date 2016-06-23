package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential

import java.util.ArrayList

import com.fasterxml.jackson.annotation.JsonProperty

class Selector {

    @JsonProperty("name")
    var name: String? = null
    @JsonProperty("parent")
    var parent: String? = null
    @JsonProperty("values")
    var values: List<Value> = ArrayList()
}
