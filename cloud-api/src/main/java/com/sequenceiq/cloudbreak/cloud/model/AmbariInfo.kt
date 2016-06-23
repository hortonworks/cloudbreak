package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AmbariInfo {

    @JsonProperty("cb_versions")
    var cbVersions: List<String>? = null
    @JsonProperty("version")
    var version: String? = null
    @JsonProperty("repo")
    var repo: Map<String, String>? = null
    @JsonProperty("hdp")
    var hdp: List<HDPInfo>? = null
}
