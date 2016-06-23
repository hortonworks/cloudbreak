package com.sequenceiq.cloudbreak.cloud.mock

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus

@Service
class MockCredentialConnector : CredentialConnector {
    override fun verify(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        val credential = authenticatedContext.cloudCredential
        return CloudCredentialStatus(credential, CredentialStatus.VERIFIED)
    }

    override fun create(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.CREATED)
    }

    override fun delete(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.DELETED)
    }
}
