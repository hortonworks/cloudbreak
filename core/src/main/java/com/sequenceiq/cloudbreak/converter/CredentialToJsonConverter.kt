package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService
import com.sequenceiq.cloudbreak.service.topology.TopologyService

@Component
class CredentialToJsonConverter : AbstractConversionServiceAwareConverter<Credential, CredentialResponse>() {

    @Inject
    private val credentialDefinitionService: CredentialDefinitionService? = null
    @Inject
    private val topologyService: TopologyService? = null

    override fun convert(source: Credential): CredentialResponse {
        val credentialJson = CredentialResponse()
        credentialJson.id = source.id
        credentialJson.cloudPlatform = source.cloudPlatform()
        credentialJson.name = source.name
        credentialJson.isPublicInAccount = source.isPublicInAccount
        if (source.attributes != null) {
            val parameters = credentialDefinitionService!!.revertProperties(Companion.platform(source.cloudPlatform()), source.attributes.map)
            credentialJson.parameters = parameters
        }
        credentialJson.description = if (source.description == null) "" else source.description
        credentialJson.publicKey = source.publicKey
        credentialJson.loginUserName = source.loginUserName
        if (source.topology != null) {
            credentialJson.topologyId = source.topology.id
        }
        clearPasswordField(credentialJson)
        return credentialJson
    }

    private fun clearPasswordField(response: CredentialResponse) {
        if (response.parameters[PASSWORD_FIELD] != null) {
            response.parameters.put(PASSWORD_FIELD, PASSWORD_PLACEHOLDER)
        }
    }

    companion object {
        private val PASSWORD_FIELD = "password"
        private val PASSWORD_PLACEHOLDER = "********"
    }
}
