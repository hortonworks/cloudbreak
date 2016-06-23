package com.sequenceiq.cloudbreak.service.clustertemplate

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.ClusterTemplate
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class ClusterTemplateService {

    @Inject
    private val clusterTemplateRepository: ClusterTemplateRepository? = null

    @Inject
    private val clusterRepository: ClusterRepository? = null


    fun retrievePrivateClusterTemplates(user: CbUser): Set<ClusterTemplate> {
        return clusterTemplateRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountClusterTemplates(user: CbUser): Set<ClusterTemplate> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return clusterTemplateRepository!!.findAllInAccount(user.account)
        } else {
            return clusterTemplateRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): ClusterTemplate {
        val clusterTemplate = clusterTemplateRepository!!.findOne(id) ?: throw NotFoundException(String.format("ClusterTemplate '%s' not found.", id))
        return clusterTemplate
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun getByName(name: String, user: CbUser): ClusterTemplate {
        val clusterTemplate = clusterTemplateRepository!!.findByNameInAccount(name, user.account, user.username) ?: throw NotFoundException(String.format("Blueprint '%s' not found.", name))
        return clusterTemplate
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, clusterTemplate: ClusterTemplate): ClusterTemplate {
        LOGGER.debug("Creating clusterTemplate: [User: '{}', Account: '{}']", user.username, user.account)
        var savedClusterTemplate: ClusterTemplate? = null
        clusterTemplate.owner = user.userId
        clusterTemplate.account = user.account
        try {
            savedClusterTemplate = clusterTemplateRepository!!.save(clusterTemplate)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.CLUSTER_TEMPLATE, clusterTemplate.name, ex)
        }

        return savedClusterTemplate
    }

    fun delete(id: Long?, user: CbUser) {
        val clusterTemplate = clusterTemplateRepository!!.findByIdInAccount(id, user.account) ?: throw NotFoundException(String.format("ClusterTemplate '%s' not found.", id))
        delete(clusterTemplate, user)
    }

    fun getPublicClusterTemplate(name: String, user: CbUser): ClusterTemplate {
        val clusterTemplate = clusterTemplateRepository!!.findOneByName(name, user.account) ?: throw NotFoundException(String.format("ClusterTemplate '%s' not found.", name))
        return clusterTemplate
    }

    fun getPrivateClusterTemplate(name: String, user: CbUser): ClusterTemplate {
        val clusterTemplate = clusterTemplateRepository!!.findByNameInUser(name, user.userId) ?: throw NotFoundException(String.format("ClusterTemplate '%s' not found.", name))
        return clusterTemplate
    }

    fun delete(name: String, user: CbUser) {
        val clusterTemplate = clusterTemplateRepository!!.findByNameInAccount(name, user.account, user.userId) ?: throw NotFoundException(String.format("ClusterTemplate '%s' not found.", name))
        delete(clusterTemplate, user)
    }

    @Transactional(Transactional.TxType.NEVER)
    fun save(entities: Iterable<ClusterTemplate>): Iterable<ClusterTemplate> {
        return clusterTemplateRepository!!.save(entities)
    }

    private fun delete(clusterTemplate: ClusterTemplate, user: CbUser) {
        if (user.userId != clusterTemplate.owner && !user.roles.contains(CbUserRole.ADMIN)) {
            throw BadRequestException("ClusterTemplate can only be deleted by account admins or owners.")
        }
        clusterTemplateRepository!!.delete(clusterTemplate)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ClusterTemplateService::class.java)
    }
}
