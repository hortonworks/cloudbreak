package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
class Image(@JsonProperty("imageName") val imageName: String,
            @JsonProperty("userdata") userdata: Map<InstanceGroupType, String>,
            @JsonProperty("hdpRepo") val hdpRepo: HDPRepo,
            @JsonProperty("hdpVersion") val hdpVersion: String) {
    val userdata: Map<InstanceGroupType, String>

    init {
        this.userdata = ImmutableMap.copyOf(userdata)
    }

    fun getUserData(key: InstanceGroupType): String {
        return userdata[key]
    }
}
