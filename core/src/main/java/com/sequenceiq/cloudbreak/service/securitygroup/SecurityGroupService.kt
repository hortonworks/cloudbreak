package com.sequenceiq.cloudbreak.service.securitygroup

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class SecurityGroupService {

    @Inject
    private val groupRepository: SecurityGroupRepository? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, securityGroup: SecurityGroup): SecurityGroup {
        LOGGER.info("Creating SecurityGroup: [User: '{}', Account: '{}']", user.username, user.account)
        securityGroup.owner = user.userId
        securityGroup.account = user.account
        try {
            return groupRepository!!.save(securityGroup)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.SECURITY_GROUP, securityGroup.name, ex)
        }

    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): SecurityGroup? {
        val securityGroup = groupRepository!!.findById(id) ?: throw NotFoundException(String.format("SecurityGroup '%s' not found", id))
        return securityGroup
    }

    fun getPrivateSecurityGroup(name: String, user: CbUser): SecurityGroup {
        val securityGroup = groupRepository!!.findByNameForUser(name, user.userId) ?: throw NotFoundException(String.format("SecurityGroup '%s' not found for user", name))
        return securityGroup
    }

    fun getPublicSecurityGroup(name: String, user: CbUser): SecurityGroup {
        val securityGroup = groupRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("SecurityGroup '%s' not found in account", name))
        return securityGroup
    }

    fun delete(id: Long?, user: CbUser) {
        LOGGER.info("Deleting SecurityGroup with id: {}", id)
        val securityGroup = get(id) ?: throw NotFoundException(String.format("SecurityGroup '%s' not found.", id))
        delete(user, securityGroup)
    }

    fun delete(name: String, user: CbUser) {
        LOGGER.info("Deleting SecurityGroup with name: {}", name)
        val securityGroup = groupRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("SecurityGroup '%s' not found.", name))
        delete(user, securityGroup)
    }

    fun retrievePrivateSecurityGroups(user: CbUser): Set<SecurityGroup> {
        return groupRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountSecurityGroups(user: CbUser): Set<SecurityGroup> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return groupRepository!!.findAllInAccount(user.account)
        } else {
            return groupRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    private fun delete(user: CbUser, securityGroup: SecurityGroup) {
        if (stackRepository!!.findAllBySecurityGroup(securityGroup.id).isEmpty()) {
            if (user.userId != securityGroup.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Public SecurityGroups can only be deleted by owners or account admins.")
            } else {
                if (ResourceStatus.USER_MANAGED == securityGroup.status) {
                    groupRepository!!.delete(securityGroup)
                } else {
                    securityGroup.status = ResourceStatus.DEFAULT_DELETED
                    groupRepository!!.save(securityGroup)
                }
            }
        } else {
            throw BadRequestException(String.format(
                    "There are clusters associated with SecurityGroup '%s'(ID:'%s'). Please remove these before deleting the SecurityGroup.",
                    securityGroup.name, securityGroup.id))
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SecurityGroupService::class.java)
    }
}
