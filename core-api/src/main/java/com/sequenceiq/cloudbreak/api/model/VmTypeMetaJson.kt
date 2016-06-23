package com.sequenceiq.cloudbreak.api.model

import java.util.ArrayList
import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class VmTypeMetaJson {
    var configs: List<VolumeParameterConfigJson> = ArrayList()
    var properties: Map<String, String> = HashMap()
}
