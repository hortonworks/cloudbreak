package com.sequenceiq.cloudbreak.cloud.aws

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.amazonaws.services.securitytoken.model.AssumeRoleResult
import com.sequenceiq.cloudbreak.cloud.aws.cache.AwsCachingConfig
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView

@Component
class AwsSessionCredentialClient {

    @Value("${cb.aws.external.id:}")
    private val externalId: String? = null

    @Inject
    private val awsEnvironmentVariableChecker: AwsEnvironmentVariableChecker? = null

    @Cacheable(value = AwsCachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE, unless = "#awsCredential.getId() == null")
    fun retrieveCachedSessionCredentials(awsCredential: AwsCredentialView): BasicSessionCredentials {
        return retrieveSessionCredentials(awsCredential)
    }

    fun retrieveSessionCredentials(awsCredential: AwsCredentialView): BasicSessionCredentials {
        LOGGER.debug("retrieving session credential")
        val client = awsSecurityTokenServiceClient()
        val assumeRoleRequest = AssumeRoleRequest().withDurationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION).withExternalId(externalId).withRoleArn(awsCredential.roleArn).withRoleSessionName("hadoop-provisioning")
        val result = client.assumeRole(assumeRoleRequest)
        return BasicSessionCredentials(
                result.credentials.accessKeyId,
                result.credentials.secretAccessKey,
                result.credentials.sessionToken)
    }


    private fun awsSecurityTokenServiceClient(): AWSSecurityTokenServiceClient {
        if (!awsEnvironmentVariableChecker!!.isAwsAccessKeyAvailable || !awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable) {
            val instanceProfileCredentialsProvider = InstanceProfileCredentialsProvider()
            LOGGER.info("AWSSecurityTokenServiceClient will use aws metadata because environment variables are undefined")
            return AWSSecurityTokenServiceClient(instanceProfileCredentialsProvider)
        } else {
            LOGGER.info("AWSSecurityTokenServiceClient will use environment variables")
            return AWSSecurityTokenServiceClient()
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AwsSessionCredentialClient::class.java)
        private val DEFAULT_SESSION_CREDENTIALS_DURATION = 3600
    }

}
