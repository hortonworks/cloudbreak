package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus

import reactor.bus.Event

@Component
class CredentialVerificationHandler : CloudPlatformEventHandler<CredentialVerificationRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<CredentialVerificationRequest> {
        return CredentialVerificationRequest::class.java
    }

    override fun accept(createCredentialRequestEvent: Event<CredentialVerificationRequest>) {
        LOGGER.info("Received event: {}", createCredentialRequestEvent)
        val request = createCredentialRequestEvent.data
        try {
            val connector = cloudPlatformConnectors!!.getDefault(request.cloudContext.platform)
            var ac: AuthenticatedContext? = null
            var cloudCredentialStatus: CloudCredentialStatus? = null
            try {
                ac = connector.authentication().authenticate(request.cloudContext, request.cloudCredential)
                cloudCredentialStatus = connector.credentials().verify(ac)
            } catch (e: CredentialVerificationException) {
                val errorMessage = e.message
                LOGGER.error(errorMessage, e)
                cloudCredentialStatus = CloudCredentialStatus(request.cloudCredential, CredentialStatus.FAILED, e, errorMessage)
            } catch (e: Exception) {
                val errorMessage = String.format("Could not verify credential [credential: '%s'], detailed message: %s",
                        request.cloudContext.name, e.message)
                LOGGER.error(errorMessage, e)
                cloudCredentialStatus = CloudCredentialStatus(request.cloudCredential, CredentialStatus.FAILED, e, errorMessage)
            }

            val credentialVerificationResult = CredentialVerificationResult(request, cloudCredentialStatus)
            request.result.onNext(credentialVerificationResult)
            LOGGER.info("Credential verification successfully finished")
        } catch (e: Exception) {
            request.result.onNext(CredentialVerificationResult(e.message, e, request))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CredentialVerificationHandler::class.java)
    }

}
