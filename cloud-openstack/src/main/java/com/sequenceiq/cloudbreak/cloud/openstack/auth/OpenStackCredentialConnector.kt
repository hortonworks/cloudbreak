package com.sequenceiq.cloudbreak.cloud.openstack.auth

import java.lang.String.format

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Keypair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView

@Service
class OpenStackCredentialConnector : CredentialConnector {

    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun verify(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        return CloudCredentialStatus(authenticatedContext.cloudCredential, CredentialStatus.VERIFIED)
    }

    override fun create(auth: AuthenticatedContext): CloudCredentialStatus {
        LOGGER.info("Create credential: {}", auth.cloudCredential)
        val client = openStackClient!!.createOSClient(auth)

        val keystoneCredential = openStackClient.createKeystoneCredential(auth)

        val keyPairName = keystoneCredential.keyPairName
        var keyPair: Keypair? = client.compute().keypairs().get(keyPairName)
        if (keyPair != null) {
            return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.FAILED, null, format("Key with name: %s already exist", keyPairName))
        }

        try {
            keyPair = client.compute().keypairs().create(keyPairName, keystoneCredential.publicKey)
            LOGGER.info("Credential has been created: {}, kp: {}", auth.cloudCredential, keyPair)
            return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.CREATED)
        } catch (e: Exception) {
            LOGGER.error("Failed to create credential", e)
            return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.FAILED, e, e.message)
        }

    }

    override fun delete(auth: AuthenticatedContext): CloudCredentialStatus {
        LOGGER.info("Delete credential: {}", auth.cloudCredential)

        val client = openStackClient!!.createOSClient(auth)
        val keystoneCredential = openStackClient.createKeystoneCredential(auth)
        val keyPairName = keystoneCredential.keyPairName

        client.compute().keypairs().delete(keyPairName)

        LOGGER.info("Credential has been deleted: {}", auth.cloudCredential)

        return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.DELETED)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenStackCredentialConnector::class.java)
    }

}
