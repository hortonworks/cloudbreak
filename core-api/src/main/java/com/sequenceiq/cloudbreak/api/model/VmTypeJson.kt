package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class VmTypeJson {
    var value: String? = null
    var vmTypeMetaJson: VmTypeMetaJson? = null

    constructor() {

    }

    constructor(value: String) {
        this.value = value
    }

    constructor(value: String, vmTypeMetaJson: VmTypeMetaJson) {
        this.value = value
        this.vmTypeMetaJson = vmTypeMetaJson
    }
}
