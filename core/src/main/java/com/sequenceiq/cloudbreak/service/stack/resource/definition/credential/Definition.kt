package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential

import java.util.ArrayList

import com.fasterxml.jackson.annotation.JsonProperty

class Definition {

    @JsonProperty("values")
    var defaultValues: List<Value> = ArrayList()
        private set
    @JsonProperty("selectors")
    var selectors: List<Selector> = ArrayList()

    fun setValues(values: List<Value>) {
        this.defaultValues = values
    }
}
