package com.sequenceiq.cloudbreak.cloud.arm

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus

@Service
class ArmCredentialConnector : CredentialConnector {

    override fun verify(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        try {
            val client = authenticatedContext.getParameter<AzureRMClient>(AzureRMClient::class.java)
            client.token
        } catch (ex: NullPointerException) {
            val message = "Invalid App ID or Tenant ID or Password"
            LOGGER.error(message, ex)
            return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.FAILED, ex, message)
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
            return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.FAILED, e, e.message)
        }

        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.VERIFIED)
    }

    override fun create(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.CREATED)
    }

    override fun delete(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.DELETED)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ArmCredentialConnector::class.java)
    }
}
