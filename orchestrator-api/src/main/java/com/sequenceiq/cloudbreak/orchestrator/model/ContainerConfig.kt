package com.sequenceiq.cloudbreak.orchestrator.model

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
class ContainerConfig(@JsonProperty("name") val name: String, @JsonProperty("version") val version: String) {

    override fun toString(): String {
        return "ContainerConfig{"
        +"name='" + name + '\''
        +", version='" + version + '\''
        +'}'
    }
}
