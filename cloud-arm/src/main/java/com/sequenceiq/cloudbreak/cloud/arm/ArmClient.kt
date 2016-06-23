package com.sequenceiq.cloudbreak.cloud.arm

import org.springframework.stereotype.Component

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

@Component
class ArmClient {

    fun createAuthenticatedContext(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        val authenticatedContext = AuthenticatedContext(cloudContext, cloudCredential)
        val azureRMClient = getClient(cloudCredential)
        authenticatedContext.putParameter(AzureRMClient::class.java, azureRMClient)
        return authenticatedContext
    }

    fun getClient(credential: CloudCredential): AzureRMClient {
        val armCredential = ArmCredentialView(credential)
        return getClient(armCredential)
    }

    fun getClient(armCredential: ArmCredentialView): AzureRMClient {
        return AzureRMClient(armCredential.tenantId, armCredential.accessKey, armCredential.secretKey, armCredential.subscriptionId)
    }


}
