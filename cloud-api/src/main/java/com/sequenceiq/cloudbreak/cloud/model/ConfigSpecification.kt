package com.sequenceiq.cloudbreak.cloud.model

import com.fasterxml.jackson.annotation.JsonProperty

class ConfigSpecification {

    @JsonProperty("volumeParameterType")
    var volumeParameterType: String? = null
    @JsonProperty("minimumSize")
    var minimumSize: String? = null
    @JsonProperty("maximumSize")
    var maximumSize: String? = null
    @JsonProperty("minimumNumber")
    var minimumNumber: String? = null
    @JsonProperty("maximumNumber")
    var maximumNumber: String? = null

    val maximumNumberWithLimit: Int?
        get() {
            val maxNumber = Integer.valueOf(maximumNumber)!!
            return if (maxNumber > LIMIT) LIMIT else maxNumber
        }

    companion object {

        private val LIMIT = 24
    }
}
