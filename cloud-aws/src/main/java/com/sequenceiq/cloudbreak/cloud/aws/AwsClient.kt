package com.sequenceiq.cloudbreak.cloud.aws

import org.apache.commons.lang3.StringUtils.isEmpty
import org.apache.commons.lang3.StringUtils.isNoneEmpty

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

@Component
class AwsClient {

    @Inject
    private val awsPlatformParameters: AwsPlatformParameters? = null

    @Inject
    private val credentialClient: AwsSessionCredentialClient? = null

    @Inject
    private val awsEnvironmentVariableChecker: AwsEnvironmentVariableChecker? = null

    fun createAuthenticatedContext(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        val authenticatedContext = AuthenticatedContext(cloudContext, cloudCredential)
        try {
            authenticatedContext.putParameter(AmazonEC2Client::class.java, createAccess(authenticatedContext.cloudCredential))
        } catch (e: AmazonServiceException) {
            throw CredentialVerificationException(e.errorMessage, e)
        }

        return authenticatedContext
    }

    fun createAccess(credential: CloudCredential): AmazonEC2Client {
        return createAccess(AwsCredentialView(credential), DEFAULT_REGION_NAME)
    }

    fun createAccess(awsCredential: AwsCredentialView, regionName: String): AmazonEC2Client {
        val client = if (isRoleAssumeRequired(awsCredential))
            AmazonEC2Client(credentialClient!!.retrieveCachedSessionCredentials(awsCredential))
        else
            AmazonEC2Client(createAwsCredentials(awsCredential))
        client.setRegion(RegionUtils.getRegion(regionName))
        return client
    }

    fun createAmazonIdentityManagement(awsCredential: AwsCredentialView): AmazonIdentityManagement {
        val iam = if (isRoleAssumeRequired(awsCredential))
            AmazonIdentityManagementClient(credentialClient!!.retrieveCachedSessionCredentials(awsCredential))
        else
            AmazonIdentityManagementClient(createAwsCredentials(awsCredential))
        return iam
    }

    fun createCloudFormationClient(awsCredential: AwsCredentialView, regionName: String): AmazonCloudFormationClient {
        val client = if (isRoleAssumeRequired(awsCredential))
            AmazonCloudFormationClient(credentialClient!!.retrieveCachedSessionCredentials(awsCredential))
        else
            AmazonCloudFormationClient(createAwsCredentials(awsCredential))
        client.setRegion(RegionUtils.getRegion(regionName))
        return client
    }

    fun createAutoScalingClient(awsCredential: AwsCredentialView, regionName: String): AmazonAutoScalingClient {
        val client = if (isRoleAssumeRequired(awsCredential))
            AmazonAutoScalingClient(credentialClient!!.retrieveCachedSessionCredentials(awsCredential))
        else
            AmazonAutoScalingClient(createAwsCredentials(awsCredential))
        client.setRegion(RegionUtils.getRegion(regionName))
        return client
    }

    fun getCbName(groupName: String, number: Long?): String {
        return String.format("%s%s", groupName, number)
    }

    fun getKeyPairName(ac: AuthenticatedContext): String {
        return String.format("%s%s%s%s", ac.cloudCredential.name, ac.cloudCredential.id,
                ac.cloudContext.name, ac.cloudContext.id)
    }

    fun existingKeyPairNameSpecified(auth: AuthenticatedContext): Boolean {
        return StringUtils.isNoneEmpty(getExistingKeyPairName(auth))
    }

    fun getExistingKeyPairName(auth: AuthenticatedContext): String {
        val cloudCredential = auth.cloudCredential
        return cloudCredential.getParameter<String>(EXISTING_KEYPAIR_PARAM_KEY, String::class.java)
    }

    fun checkAwsEnvironmentVariables(credential: CloudCredential) {
        val awsCredential = AwsCredentialView(credential)
        if (isRoleAssumeRequired(awsCredential)) {
            if (awsEnvironmentVariableChecker!!.isAwsAccessKeyAvailable && !awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable) {
                throw CloudConnectorException("If 'AWS_ACCESS_KEY_ID' available then 'AWS_SECRET_ACCESS_KEY' must be set!")
            } else if (awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable && !awsEnvironmentVariableChecker.isAwsAccessKeyAvailable) {
                throw CloudConnectorException("If 'AWS_SECRET_ACCESS_KEY' available then 'AWS_ACCESS_KEY_ID' must be set!")
            } else if (!awsEnvironmentVariableChecker.isAwsAccessKeyAvailable && !awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable) {
                try {
                    InstanceProfileCredentialsProvider().credentials
                } catch (e: AmazonClientException) {
                    val sb = StringBuilder()
                    sb.append("The 'AWS_ACCESS_KEY_ID' and 'AWS_SECRET_ACCESS_KEY' environment variables must be set ")
                    sb.append("or an instance profile role should be available.")
                    LOGGER.info(sb.toString())
                    throw CloudConnectorException(sb.toString())
                }

            }
        }
    }

    fun roleBasedCredential(awsCredential: AwsCredentialView): Boolean {
        return isNoneEmpty(awsCredential.roleArn)
    }

    private fun isRoleAssumeRequired(awsCredential: AwsCredentialView): Boolean {
        return isNoneEmpty(awsCredential.roleArn) && isEmpty(awsCredential.accessKey) && isEmpty(awsCredential.secretKey)
    }

    private fun createAwsCredentials(credentialView: AwsCredentialView): BasicAWSCredentials {
        val accessKey = credentialView.accessKey
        val secretKey = credentialView.secretKey
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            throw CloudConnectorException("Missing access or secret key from the credential.")
        }
        return BasicAWSCredentials(accessKey, secretKey)
    }

    companion object {
        private val DEFAULT_REGION_NAME = "us-west-1"
        private val LOGGER = LoggerFactory.getLogger(AwsClient::class.java)
        private val EXISTING_KEYPAIR_PARAM_KEY = "existingKeyPairName"
    }
}
