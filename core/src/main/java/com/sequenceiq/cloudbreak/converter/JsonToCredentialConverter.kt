package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.CredentialRequest
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService
import com.sequenceiq.cloudbreak.service.topology.TopologyService

@Component
class JsonToCredentialConverter : AbstractConversionServiceAwareConverter<CredentialRequest, Credential>() {

    @Inject
    private val credentialDefinitionService: CredentialDefinitionService? = null
    @Inject
    private val topologyService: TopologyService? = null

    override fun convert(source: CredentialRequest): Credential {
        val credential = Credential()
        credential.name = source.name
        credential.description = source.description
        credential.publicKey = source.publicKey
        val cloudPlatform = source.cloudPlatform
        credential.setCloudPlatform(cloudPlatform)
        val parameters = credentialDefinitionService!!.processProperties(Companion.platform(cloudPlatform), source.parameters)
        if (parameters != null && !parameters.isEmpty()) {
            try {
                credential.attributes = Json(parameters)
            } catch (e: JsonProcessingException) {
                throw BadRequestException("Invalid parameters")
            }

        }
        if (source.loginUserName != null) {
            throw BadRequestException("You can not modify the default user!")
        }
        setUserName(credential, source.parameters)
        if (source.topologyId != null) {
            credential.topology = topologyService!!.get(source.topologyId)
        }
        return credential
    }

    //TODO remove this part completely when cloudbreak user is used everywhere
    private fun setUserName(credential: Credential, parameters: Map<String, Any>) {
        if (parameters.containsKey("keystoneVersion")) {
            credential.loginUserName = SSH_USER_CENT
        } else if (parameters.containsKey("roleArn") || parameters.containsKey("accessKey") && parameters.containsKey("secretKey") && !parameters.containsKey("subscriptionId")) {
            credential.loginUserName = SSH_USER_EC2
        } else {
            credential.loginUserName = SSH_USER_CB
        }
    }

    companion object {
        private val SSH_USER_CENT = "centos"
        private val SSH_USER_CB = "cloudbreak"
        private val SSH_USER_EC2 = "ec2-user"
    }

}
