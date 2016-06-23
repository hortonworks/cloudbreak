package com.sequenceiq.cloudbreak.cloud.arm

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
class ArmConnector : CloudConnector {

    @Inject
    private val armClient: ArmClient? = null
    @Inject
    private val armResourceConnector: ArmResourceConnector? = null
    @Inject
    private val armInstanceConnector: ArmInstanceConnector? = null
    @Inject
    private val armMetadataCollector: ArmMetadataCollector? = null
    @Inject
    private val armCredentialConnector: ArmCredentialConnector? = null
    @Inject
    private val armPlatformParameters: ArmPlatformParameters? = null
    @Inject
    private val armSetup: ArmSetup? = null
    @Inject
    private val armAuthenticator: ArmAuthenticator? = null

    override fun platform(): Platform {
        return ArmConstants.AZURE_RM_PLATFORM
    }

    override fun variant(): Variant {
        return ArmConstants.AZURE_RM_VARIANT
    }

    override fun authentication(): Authenticator {
        return armAuthenticator
    }

    override fun resources(): ResourceConnector {
        return armResourceConnector
    }

    override fun instances(): InstanceConnector {
        return armInstanceConnector
    }

    override fun metadata(): MetadataCollector {
        return armMetadataCollector
    }

    override fun parameters(): PlatformParameters {
        return armPlatformParameters
    }

    override fun setup(): Setup {
        return armSetup
    }

    override fun credentials(): CredentialConnector {
        return armCredentialConnector
    }

}
