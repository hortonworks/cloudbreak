package com.sequenceiq.cloudbreak.service.securitygroup

import com.sequenceiq.cloudbreak.service.network.ExposedService.GATEWAY
import com.sequenceiq.cloudbreak.service.network.ExposedService.HTTPS
import com.sequenceiq.cloudbreak.service.network.ExposedService.SSH

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.domain.SecurityRule
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository
import com.sequenceiq.cloudbreak.service.network.NetworkUtils
import com.sequenceiq.cloudbreak.service.network.Port

@Service
class DefaultSecurityGroupCreator {

    @Inject
    private val securityGroupService: SecurityGroupService? = null

    @Inject
    private val groupRepository: SecurityGroupRepository? = null

    @Value("${cb.nginx.port:9443}")
    private val nginxPort: Int = 0

    fun createDefaultSecurityGroups(user: CbUser): Set<SecurityGroup> {
        var securityGroups: Set<SecurityGroup> = HashSet()
        val defaultNetworks = groupRepository!!.findAllDefaultInAccount(user.account)
        if (defaultNetworks.isEmpty()) {
            LOGGER.info("Creating default security groups and rules.")
            securityGroups = createDefaultSecurityGroupInstances(user)
        }
        return securityGroups
    }

    private fun createDefaultSecurityGroupInstances(user: CbUser): Set<SecurityGroup> {
        val securityGroups = HashSet<SecurityGroup>()
        //create default strict security group
        createDefaultStringSecurityGroup(user, securityGroups)
        //create default security group which opens all of the known services' ports
        createDefaultAllKnownServicesSecurityGroup(user, securityGroups)
        return securityGroups
    }

    private fun createDefaultStringSecurityGroup(user: CbUser, securityGroups: MutableSet<SecurityGroup>) {
        val strictSecurityGroupPorts = ArrayList<Port>()
        strictSecurityGroupPorts.add(Port(SSH, "22", "tcp"))
        strictSecurityGroupPorts.add(Port(HTTPS, "443", "tcp"))
        strictSecurityGroupPorts.add(Port(GATEWAY, Integer.toString(nginxPort), "tcp"))
        val strictSecurityGroupDesc = getPortsOpenDesc(strictSecurityGroupPorts)

        addSecurityGroup(user, securityGroups, "only-ssh-and-ssl", strictSecurityGroupPorts, strictSecurityGroupDesc)
    }

    private fun createDefaultAllKnownServicesSecurityGroup(user: CbUser, securityGroups: MutableSet<SecurityGroup>) {
        //new ArrayList -> otherwise the list will be the static 'ports' list from NetworkUtils and we don't want to add nginx port to 'ports' static list.
        val portsWithoutAclRules = ArrayList<Port>(NetworkUtils.portsWithoutAclRules)
        portsWithoutAclRules.add(0, Port(GATEWAY, Integer.toString(nginxPort), "tcp"))
        val allPortsOpenDesc = getPortsOpenDesc(portsWithoutAclRules)
        addSecurityGroup(user, securityGroups, "all-services-port", portsWithoutAclRules, allPortsOpenDesc)
    }

    private fun addSecurityGroup(user: CbUser, securityGroups: MutableSet<SecurityGroup>, name: String, securityGroupPorts: List<Port>, securityGroupDesc: String) {
        val onlySshAndSsl = createSecurityGroup(user, name, securityGroupDesc)
        val sshAndSslRule = createSecurityRule(concatenatePorts(securityGroupPorts), onlySshAndSsl)
        onlySshAndSsl.securityRules = HashSet(Arrays.asList(sshAndSslRule))
        securityGroups.add(securityGroupService!!.create(user, onlySshAndSsl))
    }

    private fun getPortsOpenDesc(portsWithoutAclRules: List<Port>): String {
        val allPortsOpenDescBuilder = StringBuilder()
        allPortsOpenDescBuilder.append("Open ports:")
        for (port in portsWithoutAclRules) {
            allPortsOpenDescBuilder.append(" ").append(port.port).append(" (").append(port.name).append(")")
        }
        return allPortsOpenDescBuilder.toString()
    }

    private fun createSecurityGroup(user: CbUser, name: String, description: String): SecurityGroup {
        val securityGroup = SecurityGroup()
        securityGroup.name = name
        securityGroup.owner = user.userId
        securityGroup.account = user.account
        securityGroup.description = description
        securityGroup.isPublicInAccount = true
        securityGroup.status = ResourceStatus.DEFAULT
        return securityGroup
    }

    private fun createSecurityRule(ports: String, securityGroup: SecurityGroup): SecurityRule {
        val securityRule = SecurityRule()
        securityRule.cidr = "0.0.0.0/0"
        securityRule.isModifiable = false
        securityRule.setPorts(ports)
        securityRule.protocol = TCP_PROTOCOL
        securityRule.securityGroup = securityGroup
        return securityRule
    }

    private fun concatenatePorts(ports: List<Port>): String {
        val builder = StringBuilder("")
        val portsIterator = ports.iterator()
        while (portsIterator.hasNext()) {
            val port = portsIterator.next()
            builder.append(port.port)
            if (portsIterator.hasNext()) {
                builder.append(",")
            }
        }
        return builder.toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DefaultSecurityGroupCreator::class.java)
        private val TCP_PROTOCOL = "tcp"
    }
}
