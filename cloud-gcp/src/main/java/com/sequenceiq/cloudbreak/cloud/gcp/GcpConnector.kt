package com.sequenceiq.cloudbreak.cloud.gcp

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
class GcpConnector : CloudConnector {

    @Inject
    private val authenticator: GcpAuthenticator? = null
    @Inject
    private val provisionSetup: GcpProvisionSetup? = null
    @Inject
    private val instanceConnector: GcpInstanceConnector? = null
    @Inject
    private val resourceConnector: GcpResourceConnector? = null
    @Inject
    private val gcpCredentialConnector: GcpCredentialConnector? = null
    @Inject
    private val gcpPlatformParameters: GcpPlatformParameters? = null
    @Inject
    private val metadataCollector: GcpMetadataCollector? = null

    override fun authentication(): Authenticator {
        return authenticator
    }

    override fun setup(): Setup {
        return provisionSetup
    }

    override fun credentials(): CredentialConnector {
        return gcpCredentialConnector
    }

    override fun resources(): ResourceConnector {
        return resourceConnector
    }

    override fun instances(): InstanceConnector {
        return instanceConnector
    }

    override fun metadata(): MetadataCollector {
        return metadataCollector
    }

    override fun parameters(): PlatformParameters {
        return gcpPlatformParameters
    }

    override fun platform(): Platform {
        return GcpConstants.GCP_PLATFORM
    }

    override fun variant(): Variant {
        return GcpConstants.GCP_VARIANT
    }

}
