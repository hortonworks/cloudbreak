package com.sequenceiq.cloudbreak.cloud.aws

import org.apache.commons.lang3.StringUtils.isEmpty
import org.apache.commons.lang3.StringUtils.isNoneEmpty

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.amazonaws.AmazonClientException
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest
import com.amazonaws.services.ec2.model.ImportKeyPairRequest
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus

@Service
class AwsCredentialConnector : CredentialConnector {

    @Inject
    private val credentialClient: AwsSessionCredentialClient? = null
    @Inject
    private val awsClient: AwsClient? = null
    @Inject
    private val smartSenseIdGenerator: AwsSmartSenseIdGenerator? = null

    override fun verify(authenticatedContext: AuthenticatedContext): CloudCredentialStatus {
        val credential = authenticatedContext.cloudCredential
        LOGGER.info("Create credential: {}", credential)
        val awsCredential = AwsCredentialView(credential)
        val roleArn = awsCredential.roleArn
        val accessKey = awsCredential.accessKey
        val secretKey = awsCredential.secretKey
        val smartSenseId = smartSenseIdGenerator!!.getSmartSenseId(awsCredential)
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            credential.putParameter("smartSenseId", smartSenseId)
        }
        if (isNoneEmpty(roleArn) && isNoneEmpty(accessKey) && isNoneEmpty(secretKey)) {
            val message = "Please only provide the 'role arn' or the 'access' and 'secret key'"
            return CloudCredentialStatus(credential, CredentialStatus.FAILED, Exception(message), message)
        }
        if (isNoneEmpty(roleArn)) {
            return verifyIamRoleIsAssumable(credential)
        }
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            val message = "Please provide both the 'access' and 'secret key'"
            return CloudCredentialStatus(credential, CredentialStatus.FAILED, Exception(message), message)
        }
        return CloudCredentialStatus(credential, CredentialStatus.VERIFIED)
    }

    override fun create(auth: AuthenticatedContext): CloudCredentialStatus {
        val awsCredential = AwsCredentialView(auth.cloudCredential)
        val region = auth.cloudContext.location!!.region.value()
        if (!awsClient!!.existingKeyPairNameSpecified(auth)) {
            try {
                LOGGER.info(String.format("Importing public key to %s region on AWS", region))
                val client = awsClient.createAccess(awsCredential, region)
                val importKeyPairRequest = ImportKeyPairRequest(awsClient.getKeyPairName(auth), awsCredential.publicKey)
                client.importKeyPair(importKeyPairRequest)
            } catch (e: Exception) {
                val errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.roleArn,
                        e.message)
                LOGGER.error(errorMessage, e)
                return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.FAILED, e, errorMessage)
            }

        }
        return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.CREATED)
    }

    override fun delete(auth: AuthenticatedContext): CloudCredentialStatus {
        val awsCredential = AwsCredentialView(auth.cloudCredential)
        val region = auth.cloudContext.location!!.region.value()
        if (!awsClient!!.existingKeyPairNameSpecified(auth)) {
            try {
                val client = awsClient.createAccess(awsCredential, region)
                val deleteKeyPairRequest = DeleteKeyPairRequest(awsClient.getKeyPairName(auth))
                client.deleteKeyPair(deleteKeyPairRequest)
            } catch (e: Exception) {
                val errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        awsCredential.roleArn, region, e.message)
                LOGGER.error(errorMessage, e)
            }

        }
        return CloudCredentialStatus(auth.cloudCredential, CredentialStatus.DELETED)
    }

    private fun verifyIamRoleIsAssumable(cloudCredential: CloudCredential): CloudCredentialStatus {
        val awsCredential = AwsCredentialView(cloudCredential)
        try {
            credentialClient!!.retrieveSessionCredentials(awsCredential)
        } catch (ae: AmazonClientException) {
            if (ae.message.contains("Unable to load AWS credentials")) {
                val errorMessage = String.format("Unable to load AWS credentials: please make sure the deployer defined AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY")
                LOGGER.error(errorMessage, ae)
                return CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, ae, errorMessage)
            }
        } catch (e: Exception) {
            val errorMessage = String.format("Could not assume role '%s': check if the role exists and if it's created with the correct external ID",
                    awsCredential.roleArn)
            LOGGER.error(errorMessage, e)
            return CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage)
        }

        return CloudCredentialStatus(cloudCredential, CredentialStatus.CREATED)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AwsCredentialConnector::class.java)
    }
}
