package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.service.securitygroup.DefaultSecurityGroupCreator
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService

@Component
class SecurityGroupController : SecurityGroupEndpoint {
    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val securityGroupService: SecurityGroupService? = null

    @Autowired
    private val defaultSecurityGroupCreator: DefaultSecurityGroupCreator? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun postPrivate(securityGroupJson: SecurityGroupJson): IdJson {
        val user = authenticatedUserService!!.cbUser
        return createSecurityGroup(user, securityGroupJson, false)
    }

    override fun postPublic(securityGroupJson: SecurityGroupJson): IdJson {
        val user = authenticatedUserService!!.cbUser
        return createSecurityGroup(user, securityGroupJson, true)
    }

    override fun getPrivates(): Set<SecurityGroupJson> {
        val user = authenticatedUserService!!.cbUser
        defaultSecurityGroupCreator!!.createDefaultSecurityGroups(user)
        val securityGroups = securityGroupService!!.retrievePrivateSecurityGroups(user)
        return convert(securityGroups)
    }

    override fun getPublics(): Set<SecurityGroupJson> {
        val user = authenticatedUserService!!.cbUser
        defaultSecurityGroupCreator!!.createDefaultSecurityGroups(user)
        val securityGroups = securityGroupService!!.retrieveAccountSecurityGroups(user)
        return convert(securityGroups)
    }

    override fun get(id: Long?): SecurityGroupJson {
        val securityGroup = securityGroupService!!.get(id)
        return convert(securityGroup)
    }

    override fun getPrivate(name: String): SecurityGroupJson {
        val user = authenticatedUserService!!.cbUser
        val securityGroup = securityGroupService!!.getPrivateSecurityGroup(name, user)
        return convert(securityGroup)
    }

    override fun getPublic(name: String): SecurityGroupJson {
        val user = authenticatedUserService!!.cbUser
        val securityGroup = securityGroupService!!.getPublicSecurityGroup(name, user)
        return convert(securityGroup)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        securityGroupService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        securityGroupService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        securityGroupService!!.delete(name, user)
    }

    private fun createSecurityGroup(user: CbUser, securityGroupJson: SecurityGroupJson, publicInAccount: Boolean): IdJson {
        var securityGroup = convert(securityGroupJson, publicInAccount)
        securityGroup = securityGroupService!!.create(user, securityGroup)
        return IdJson(securityGroup.id)
    }

    private fun convert(securityGroupJson: SecurityGroupJson, publicInAccount: Boolean): SecurityGroup {
        val securityGroup = conversionService!!.convert<SecurityGroup>(securityGroupJson, SecurityGroup::class.java)
        securityGroup.isPublicInAccount = publicInAccount
        return securityGroup
    }

    private fun convert(securityGroup: SecurityGroup): SecurityGroupJson {
        return conversionService!!.convert<SecurityGroupJson>(securityGroup, SecurityGroupJson::class.java)
    }

    private fun convert(securityGroups: Set<SecurityGroup>): Set<SecurityGroupJson> {
        val jsons = HashSet<SecurityGroupJson>()
        for (securityGroup in securityGroups) {
            jsons.add(convert(securityGroup))
        }
        return jsons
    }
}
