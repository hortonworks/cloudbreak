package com.sequenceiq.cloudbreak.cloud.aws

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

@Service
class AwsAuthenticator : Authenticator {

    @Inject
    private val awsClient: AwsClient? = null

    override fun platform(): Platform {
        return AwsConstants.AWS_PLATFORM
    }

    override fun variant(): Variant {
        return AwsConstants.AWS_VARIANT
    }

    override fun authenticate(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        LOGGER.info("Authenticating to aws ...")
        awsClient!!.checkAwsEnvironmentVariables(cloudCredential)
        return awsClient.createAuthenticatedContext(cloudContext, cloudCredential)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AwsAuthenticator::class.java)
    }
}
