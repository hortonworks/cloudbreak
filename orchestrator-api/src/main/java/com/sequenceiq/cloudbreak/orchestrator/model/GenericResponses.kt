package com.sequenceiq.cloudbreak.orchestrator.model

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException

class GenericResponses {

    var responses: List<GenericResponse>? = null

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun assertError() {
        for (resp in responses!!) {
            resp.assertError()
        }
    }

    override fun toString(): String {
        val sb = StringBuilder("SaltBootResponses{")
        sb.append("responses=").append(responses)
        sb.append('}')
        return sb.toString()
    }
}
