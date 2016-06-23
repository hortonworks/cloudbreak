package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network

import javax.inject.Inject

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.compute.ComputeSecurityGroupService
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.compute.IPProtocol
import org.openstack4j.model.compute.SecGroupExtension
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackSecurityGroupResourceBuilder : AbstractOpenStackNetworkResourceBuilder() {

    @Inject
    private val utils: OpenStackUtils? = null

    @Throws(Exception::class)
    override fun build(context: OpenStackContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource {
        try {
            val osClient = createOSClient(auth)
            val securityGroupService = osClient.compute().securityGroups()
            val securityGroup = securityGroupService.create(resource.name, "")
            val securityGroupId = securityGroup.id
            for (rule in security.rules) {
                val osProtocol = getProtocol(rule.protocol)
                val cidr = rule.cidr
                for (portStr in rule.ports) {
                    val port = Integer.valueOf(portStr)!!
                    securityGroupService.createRule(createRule(securityGroupId, osProtocol, cidr, port, port))
                }
            }
            val subnetCidr = if (utils!!.isExistingSubnet(network)) utils.getExistingSubnetCidr(auth, network) else network.subnet.cidr
            securityGroupService.createRule(createRule(securityGroupId, IPProtocol.TCP, subnetCidr, MIN_PORT, MAX_PORT))
            securityGroupService.createRule(createRule(securityGroupId, IPProtocol.UDP, subnetCidr, MIN_PORT, MAX_PORT))
            securityGroupService.createRule(createRule(securityGroupId, IPProtocol.ICMP, "0.0.0.0/0"))
            context.putParameter(OpenStackConstants.SECURITYGROUP_ID, securityGroupId)
            return createPersistedResource(resource, securityGroup.id)
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("SecurityGroup creation failed", resourceType(), resource.name, ex)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource {
        try {
            val osClient = createOSClient(auth)
            val response = osClient.compute().securityGroups().delete(resource.reference)
            return checkDeleteResponse(response, resourceType(), auth, resource, "SecurityGroup deletion failed")
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("SecurityGroup deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_SECURITY_GROUP
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        return true
    }

    private fun createRule(securityGroupId: String, protocol: IPProtocol, cidr: String, fromPort: Int, toPort: Int): SecGroupExtension.Rule {
        return Builders.secGroupRule().parentGroupId(securityGroupId).protocol(protocol).cidr(cidr).range(fromPort, toPort).build()
    }

    private fun createRule(securityGroupId: String, protocol: IPProtocol, cidr: String): SecGroupExtension.Rule {
        return Builders.secGroupRule().parentGroupId(securityGroupId).protocol(protocol).cidr(cidr).build()
    }

    private fun getProtocol(protocolStr: String): IPProtocol {
        return IPProtocol.value(protocolStr)
    }

    companion object {
        private val MAX_PORT = 65535
        private val MIN_PORT = 1
    }
}
