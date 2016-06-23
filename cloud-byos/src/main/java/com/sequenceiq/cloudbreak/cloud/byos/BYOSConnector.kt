package com.sequenceiq.cloudbreak.cloud.byos

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
class BYOSConnector : CloudConnector {

    @Inject
    private val platformParameters: BYOSPlatformParameters? = null

    override fun authentication(): Authenticator {
        throw UnsupportedOperationException("Authentication operation is not supported on BYOS stacks.")
    }

    override fun setup(): Setup {
        throw UnsupportedOperationException("Setup operation is not supported on BYOS stacks.")
    }

    override fun credentials(): CredentialConnector {
        throw UnsupportedOperationException("Credentials operation is not supported on BYOS stacks.")
    }

    override fun resources(): ResourceConnector {
        throw UnsupportedOperationException("Resources operation is not supported on BYOS stacks.")
    }

    override fun instances(): InstanceConnector {
        throw UnsupportedOperationException("Instances operation is not supported on BYOS stacks.")
    }

    override fun metadata(): MetadataCollector {
        throw UnsupportedOperationException("Metadata operation is not supported on BYOS stacks.")
    }

    override fun parameters(): PlatformParameters {
        return platformParameters
    }

    override fun platform(): Platform {
        return BYOSConstants.BYOS_PLATFORM
    }

    override fun variant(): Variant {
        return BYOSConstants.BYOS_VARIANT
    }
}
