package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder

@Service
class OpenStackContextBuilder : ResourceContextBuilder<OpenStackContext> {

    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun contextInit(cloudContext: CloudContext, auth: AuthenticatedContext, network: Network?, resources: List<CloudResource>?, build: Boolean): OpenStackContext {
        val osClient = openStackClient!!.createOSClient(auth)
        val credentialView = KeystoneCredentialView(auth)

        val openStackContext = OpenStackContext(cloudContext.name, cloudContext.location,
                PARALLEL_RESOURCE_REQUEST, build)

        openStackContext.putParameter(OpenStackConstants.TENANT_ID, osClient.identity().tenants().getByName(credentialView.tenantName).id)

        if (resources != null) {
            for (resource in resources) {
                when (resource.type) {
                    ResourceType.OPENSTACK_SUBNET -> openStackContext.putParameter(OpenStackConstants.SUBNET_ID, resource.reference)
                    ResourceType.OPENSTACK_NETWORK -> openStackContext.putParameter(OpenStackConstants.NETWORK_ID, resource.reference)
                    ResourceType.OPENSTACK_SECURITY_GROUP -> openStackContext.putParameter(OpenStackConstants.SECURITYGROUP_ID, resource.reference)
                    else -> LOGGER.debug("Resource is not used during context build: {}", resource)
                }
            }
        }
        openStackContext.putParameter(OpenStackConstants.FLOATING_IP_IDS, Collections.synchronizedList(ArrayList<String>()))
        if (network != null) {
            openStackContext.putParameter(OpenStackConstants.PUBLIC_NET_ID, network.getStringParameter(OpenStackConstants.PUBLIC_NET_ID))
        }

        return openStackContext
    }

    override fun platform(): Platform {
        return OpenStackConstants.OPENSTACK_PLATFORM
    }

    override fun variant(): Variant {
        return OpenStackConstants.OpenStackVariant.NATIVE.variant()
    }

    companion object {
        private val PARALLEL_RESOURCE_REQUEST = 30

        private val LOGGER = LoggerFactory.getLogger(OpenStackContextBuilder::class.java)
    }
}
