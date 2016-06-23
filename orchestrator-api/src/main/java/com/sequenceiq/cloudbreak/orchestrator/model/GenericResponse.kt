package com.sequenceiq.cloudbreak.orchestrator.model

import org.springframework.http.HttpStatus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException

@JsonIgnoreProperties(ignoreUnknown = true)
class GenericResponse {

    var status: String? = null
    var address: String? = null
    var statusCode: Int = 0
    var version: String? = null
    var errorText: String? = null

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun assertError() {
        if (statusCode != HttpStatus.OK.value()) {
            throw CloudbreakOrchestratorFailedException(toString())
        }
    }

    override fun toString(): String {
        val sb = StringBuilder("SaltBootResponse{")
        sb.append("status='").append(status).append('\'')
        sb.append(", address='").append(address).append('\'')
        sb.append(", statusCode=").append(statusCode)
        sb.append(", version='").append(version).append('\'')
        sb.append(", errorText='").append(errorText).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
