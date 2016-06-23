package com.sequenceiq.cloudbreak.service.stack.connector.adapter

import java.io.IOException

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.service.credential.OpenSshPublicKeyValidator
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ServiceProviderCredentialAdapter {

    @Inject
    private val eventBus: EventBus? = null

    @Inject
    private val rsaPublicKeyValidator: OpenSshPublicKeyValidator? = null

    @Inject
    private val credentialConverter: CredentialToCloudCredentialConverter? = null

    fun init(credential: Credential): Credential {
        if (!credential.passwordAuthenticationRequired()) {
            rsaPublicKeyValidator!!.validate(credential.publicKey)
        }
        val cloudContext = CloudContext(credential.id, credential.name, credential.cloudPlatform(), credential.owner)
        val cloudCredential = credentialConverter!!.convert(credential)

        val request = CredentialVerificationRequest(cloudContext, cloudCredential)
        LOGGER.info("Triggering event: {}", request)
        eventBus!!.notify(request.selector(), Event.wrap(request))
        try {
            val res = request.await()
            val message = "Failed to verify the credential: "
            LOGGER.info("Result: {}", res)
            if (res.status !== EventStatus.OK) {
                LOGGER.error(message, res.errorDetails)
                throw BadRequestException(message + res.errorDetails!!, res.errorDetails)
            }
            if (CredentialStatus.FAILED == res.cloudCredentialStatus.status) {
                throw BadRequestException(message + res.cloudCredentialStatus.statusReason!!,
                        res.cloudCredentialStatus.exception)
            }
            val cloudCredentialResponse = res.cloudCredentialStatus.cloudCredential
            mergeSmartSenseAttributeIfExists(credential, cloudCredentialResponse)
        } catch (e: InterruptedException) {
            LOGGER.error("Error while executing credential verification", e)
            throw OperationException(e)
        }

        return credential
    }

    @Throws(Exception::class)
    fun update(credential: Credential): Credential {
        return credential
    }

    private fun mergeSmartSenseAttributeIfExists(credential: Credential, cloudCredentialResponse: CloudCredential) {
        val smartSenseId = cloudCredentialResponse.getParameters()["smartSenseId"].toString()
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            try {
                val attributes = credential.attributes
                val newAttributes = attributes.map
                newAttributes.put("smartSenseId", smartSenseId)
                credential.attributes = Json(newAttributes)
            } catch (e: IOException) {
                LOGGER.error("SmartSense id could not be added to the credential as attribute.", e)
            }

        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ServiceProviderCredentialAdapter::class.java)
    }
}
