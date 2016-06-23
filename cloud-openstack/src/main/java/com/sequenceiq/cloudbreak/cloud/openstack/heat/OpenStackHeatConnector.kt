package com.sequenceiq.cloudbreak.cloud.openstack.heat

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
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackAuthenticator
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackCredentialConnector
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackParameters
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackSetup

@Service
class OpenStackHeatConnector : CloudConnector {

    @Inject
    private val credentialConnector: OpenStackCredentialConnector? = null
    @Inject
    private val authenticator: OpenStackAuthenticator? = null
    @Inject
    private val resourceConnector: OpenStackResourceConnector? = null
    @Inject
    private val instanceConnector: OpenStackInstanceConnector? = null
    @Inject
    private val metadataCollector: OpenStackMetadataCollector? = null
    @Inject
    private val openStackSetup: OpenStackSetup? = null
    @Inject
    private val openStackParameters: OpenStackParameters? = null

    override fun platform(): Platform {
        return OpenStackConstants.OPENSTACK_PLATFORM
    }

    override fun variant(): Variant {
        return OpenStackConstants.OpenStackVariant.HEAT.variant()
    }

    override fun authentication(): Authenticator {
        return authenticator
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
        return openStackParameters
    }

    override fun setup(): Setup {
        return openStackSetup
    }

    override fun credentials(): CredentialConnector {
        return credentialConnector
    }
}
