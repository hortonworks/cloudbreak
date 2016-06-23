package com.sequenceiq.cloudbreak.converter.spi

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform

import java.util.Collections

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService

@Component
class CredentialToCloudCredentialConverter {

    @Inject
    private val definitionService: CredentialDefinitionService? = null

    fun convert(credential: Credential?): CloudCredential? {
        if (credential == null) {
            return null
        }
        val attributes = credential.attributes
        var fields: MutableMap<String, Any> = if (attributes == null) emptyMap<String, Any>() else attributes.map
        fields = definitionService!!.revertProperties(Companion.platform(credential.cloudPlatform()), fields)
        fields.put(CREDENTIAL_ID, credential.id)
        return CloudCredential(credential.id, credential.name, credential.publicKey, credential.loginUserName, fields)
    }

    companion object {

        private val CREDENTIAL_ID = "id"
    }

}
