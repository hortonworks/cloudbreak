package com.sequenceiq.cloudbreak.cloud.gcp.network

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getCustomNetworkId
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.legacyNetwork
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newNetworkAndSubnet
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newSubnetInExistingNetwork

import java.util.Arrays

import org.springframework.stereotype.Service

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Firewall
import com.google.api.services.compute.model.Operation
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class GcpFirewallInternalResourceBuilder : AbstractGcpNetworkBuilder() {

    override fun create(context: GcpContext, auth: AuthenticatedContext, network: Network): CloudResource {
        val resourceName = resourceNameService.resourceName(resourceType(), context.name)
        return createNamedResource(resourceType(), resourceName)
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, auth: AuthenticatedContext, network: Network, security: Security, buildableResource: CloudResource): CloudResource {
        val projectId = context.projectId

        val firewall = Firewall()
        val allowed1 = Firewall.Allowed()
        allowed1.ipProtocol = "tcp"
        allowed1.ports = Arrays.asList("1-65535")

        val allowed2 = Firewall.Allowed()
        allowed2.ipProtocol = "icmp"

        val allowed3 = Firewall.Allowed()
        allowed3.ipProtocol = "udp"
        allowed3.ports = Arrays.asList("1-65535")

        firewall.targetTags = Arrays.asList(GcpStackUtil.getClusterTag(auth.cloudContext))
        firewall.allowed = Arrays.asList(allowed1, allowed2, allowed3)
        firewall.name = buildableResource.name
        if (legacyNetwork(network)) {
            val networkRequest = context.compute.networks().get(projectId, getCustomNetworkId(network))
            val existingNetwork = networkRequest.execute()
            firewall.sourceRanges = Arrays.asList(existingNetwork.iPv4Range)
        } else if (newNetworkAndSubnet(network) || newSubnetInExistingNetwork(network)) {
            firewall.sourceRanges = Arrays.asList(network.subnet.cidr)
        } else {
            val sn = context.compute.subnetworks().get(projectId, context.location.region.value(), getSubnetId(network))
            val existingSubnet = sn.execute()
            firewall.sourceRanges = Arrays.asList(existingSubnet.ipCidrRange)
        }
        firewall.network = String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId,
                context.getParameter<String>(GcpNetworkResourceBuilder.NETWORK_NAME, String::class.java))

        val firewallInsert = context.compute.firewalls().insert(projectId, firewall)
        try {
            val operation = firewallInsert.execute()
            if (operation.httpErrorStatusCode != null) {
                throw GcpResourceException(operation.httpErrorMessage, resourceType(), buildableResource.name)
            }
            return createOperationAwareCloudResource(buildableResource, operation)
        } catch (e: GoogleJsonResponseException) {
            throw GcpResourceException(checkException(e), resourceType(), buildableResource.name)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: GcpContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        try {
            val operation = context.compute.firewalls().delete(context.projectId, resource.name).execute()
            return createOperationAwareCloudResource(resource, operation)
        } catch (e: GoogleJsonResponseException) {
            exceptionHandler(e, resource.name, resourceType())
            return null
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.GCP_FIREWALL_INTERNAL
    }

    override fun order(): Int {
        return 2
    }

}
