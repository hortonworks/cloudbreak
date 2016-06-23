package com.sequenceiq.cloudbreak.service.blueprint

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
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.repository.BlueprintRepository
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class BlueprintService {

    @Inject
    private val blueprintRepository: BlueprintRepository? = null

    @Inject
    private val clusterRepository: ClusterRepository? = null


    fun retrievePrivateBlueprints(user: CbUser): Set<Blueprint> {
        return blueprintRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountBlueprints(user: CbUser): Set<Blueprint> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return blueprintRepository!!.findAllInAccount(user.account)
        } else {
            return blueprintRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Blueprint {
        val blueprint = blueprintRepository!!.findOne(id) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", id))
        return blueprint
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun getByName(name: String, user: CbUser): Blueprint {
        val blueprint = blueprintRepository!!.findByNameInAccount(name, user.account, user.username) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", name))
        return blueprint
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, blueprint: Blueprint): Blueprint {
        LOGGER.debug("Creating blueprint: [User: '{}', Account: '{}']", user.username, user.account)
        var savedBlueprint: Blueprint? = null
        blueprint.owner = user.userId
        blueprint.account = user.account
        try {
            savedBlueprint = blueprintRepository!!.save(blueprint)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.BLUEPRINT, blueprint.name, ex)
        }

        return savedBlueprint
    }

    fun delete(id: Long?, user: CbUser) {
        val blueprint = blueprintRepository!!.findByIdInAccount(id, user.account) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", id))
        delete(blueprint, user)
    }

    fun getPublicBlueprint(name: String, user: CbUser): Blueprint {
        val blueprint = blueprintRepository!!.findOneByName(name, user.account) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", name))
        return blueprint
    }

    fun getPrivateBlueprint(name: String, user: CbUser): Blueprint {
        val blueprint = blueprintRepository!!.findByNameInUser(name, user.userId) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", name))
        return blueprint
    }

    fun delete(name: String, user: CbUser) {
        val blueprint = blueprintRepository!!.findByNameInAccount(name, user.account, user.userId) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", name))
        delete(blueprint, user)
    }

    @Transactional(Transactional.TxType.NEVER)
    fun save(entities: Iterable<Blueprint>): Iterable<Blueprint> {
        return blueprintRepository!!.save(entities)
    }

    private fun delete(blueprint: Blueprint, user: CbUser) {
        if (clusterRepository!!.findAllClustersByBlueprint(blueprint.id).isEmpty()) {
            if (user.userId != blueprint.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Blueprints can only be deleted by account admins or owners.")
            }
            if (ResourceStatus.USER_MANAGED == blueprint.status) {
                blueprintRepository!!.delete(blueprint)
            } else {
                blueprint.status = ResourceStatus.DEFAULT_DELETED
                blueprintRepository!!.save(blueprint)
            }
        } else {
            throw BadRequestException(String.format(
                    "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint.", blueprint.id))
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(BlueprintService::class.java)
    }
}
