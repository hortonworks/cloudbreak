package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class VolumeParameterConfigJson {
    var volumeParameterType: String? = null
    var minimumSize: Int? = null
    var maximumSize: Int? = null
    var minimumNumber: Int? = null
    var maximumNumber: Int? = null
}
