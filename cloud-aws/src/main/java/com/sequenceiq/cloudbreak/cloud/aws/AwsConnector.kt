package com.sequenceiq.cloudbreak.cloud.aws

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

@Service
class AwsConnector : CloudConnector {

    @Inject
    private val awsResourceConnector: AwsResourceConnector? = null
    @Inject
    private val awsInstanceConnector: AwsInstanceConnector? = null
    @Inject
    private val awsMetadataCollector: AwsMetadataCollector? = null
    @Inject
    private val awsCredentialConnector: AwsCredentialConnector? = null
    @Inject
    private val awsPlatformParameters: AwsPlatformParameters? = null
    @Inject
    private val awsSetup: AwsSetup? = null
    @Inject
    private val awsAuthenticator: AwsAuthenticator? = null

    override fun platform(): Platform {
        return AwsConstants.AWS_PLATFORM
    }

    override fun variant(): Variant {
        return AwsConstants.AWS_VARIANT
    }

    override fun authentication(): Authenticator {
        return awsAuthenticator
    }

    override fun resources(): ResourceConnector {
        return awsResourceConnector
    }

    override fun instances(): InstanceConnector {
        return awsInstanceConnector
    }

    override fun metadata(): MetadataCollector {
        return awsMetadataCollector
    }

    override fun parameters(): PlatformParameters {
        return awsPlatformParameters
    }

    override fun setup(): Setup {
        return awsSetup
    }

    override fun credentials(): CredentialConnector {
        return awsCredentialConnector
    }

}
