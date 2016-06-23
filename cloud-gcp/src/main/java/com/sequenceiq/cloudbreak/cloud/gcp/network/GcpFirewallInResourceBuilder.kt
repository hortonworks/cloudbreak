package com.sequenceiq.cloudbreak.cloud.gcp.network

import com.sequenceiq.cloudbreak.common.type.ResourceType.GCP_FIREWALL_IN
import java.util.Arrays.asList

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedList

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
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class GcpFirewallInResourceBuilder : AbstractGcpNetworkBuilder() {

    override fun create(context: GcpContext, auth: AuthenticatedContext, network: Network): CloudResource {
        val resourceName = resourceNameService.resourceName(resourceType(), context.name)
        return createNamedResource(resourceType(), resourceName)
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, auth: AuthenticatedContext, network: Network, security: Security, buildableResource: CloudResource): CloudResource {
        val projectId = context.projectId

        val sourceRanges = getSourceRanges(security)

        val firewall = Firewall()
        firewall.sourceRanges = sourceRanges

        val allowedRules = ArrayList<Firewall.Allowed>()
        allowedRules.add(Firewall.Allowed().setIPProtocol("icmp"))

        allowedRules.addAll(createRule(security))

        firewall.targetTags = Arrays.asList(GcpStackUtil.getClusterTag(auth.cloudContext))
        firewall.allowed = allowedRules
        firewall.name = buildableResource.name
        firewall.network = String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                projectId, context.getParameter<String>(GcpNetworkResourceBuilder.NETWORK_NAME, String::class.java))

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
    override fun update(ctx: GcpContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResourceStatus? {
        val projectId = ctx.projectId
        val compute = ctx.compute
        val resourceName = resource.name
        try {
            val fireWall = compute.firewalls().get(projectId, resourceName).execute()
            val sourceRanges = getSourceRanges(security)
            fireWall.sourceRanges = sourceRanges
            val operation = compute.firewalls().update(projectId, resourceName, fireWall).execute()
            val cloudResource = createOperationAwareCloudResource(resource, operation)
            return checkResources(ctx, auth, asList(cloudResource))[0]
        } catch (e: IOException) {
            throw GcpResourceException("Failed to update resource!", GCP_FIREWALL_IN, resourceName, e)
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
        return ResourceType.GCP_FIREWALL_IN
    }

    override fun order(): Int {
        return ORDER
    }

    private fun getSourceRanges(security: Security): List<String> {
        val rules = security.rules
        val sourceRanges = ArrayList<String>(rules.size)
        for (securityRule in rules) {
            sourceRanges.add(securityRule.cidr)
        }
        return sourceRanges
    }

    private fun createRule(security: Security): List<Firewall.Allowed> {
        val rules = LinkedList<Firewall.Allowed>()
        val securityRules = security.rules
        for (securityRule in securityRules) {
            val rule = Firewall.Allowed()
            rule.ipProtocol = securityRule.protocol
            rule.ports = asList(*securityRule.ports)
            rules.add(rule)
        }
        return rules
    }

    companion object {

        private val ORDER = 3
    }
}
