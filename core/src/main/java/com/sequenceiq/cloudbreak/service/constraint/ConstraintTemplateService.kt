package com.sequenceiq.cloudbreak.service.constraint


import java.util.Date

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class ConstraintTemplateService {

    @Inject
    private val constraintTemplateRepository: ConstraintTemplateRepository? = null

    @Inject
    private val clusterRepository: ClusterRepository? = null

    fun retrievePrivateConstraintTemplates(user: CbUser): Set<ConstraintTemplate> {
        return constraintTemplateRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountConstraintTemplates(user: CbUser): Set<ConstraintTemplate> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return constraintTemplateRepository!!.findAllInAccount(user.account)
        } else {
            return constraintTemplateRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): ConstraintTemplate {
        val constraintTemplate = constraintTemplateRepository!!.findOne(id)
        if (constraintTemplate == null) {
            throw NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, id))
        } else {
            return constraintTemplate
        }
    }

    fun create(user: CbUser, constraintTemplate: ConstraintTemplate): ConstraintTemplate {
        LOGGER.debug("Creating constraint template: [User: '{}', Account: '{}']", user.username, user.account)
        constraintTemplate.owner = user.userId
        constraintTemplate.account = user.account
        try {
            return constraintTemplateRepository!!.save(constraintTemplate)
        } catch (e: Exception) {
            throw DuplicateKeyValueException(APIResourceType.CONSTRAINT_TEMPLATE, constraintTemplate.name, e)
        }

    }

    fun delete(name: String, user: CbUser) {
        val constraintTemplate = constraintTemplateRepository!!.findByNameInAccount(name, user.account, user.userId) ?: throw NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, name))
        delete(constraintTemplate, user)
    }

    fun delete(id: Long?, user: CbUser) {
        val constraintTemplate = constraintTemplateRepository!!.findByIdInAccount(id, user.account) ?: throw NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, id))
        delete(constraintTemplate, user)
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun getPublicTemplate(name: String, user: CbUser): ConstraintTemplate {
        val constraintTemplate = constraintTemplateRepository!!.findOneByName(name, user.account)
        if (constraintTemplate == null) {
            throw NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, name))
        } else {
            return constraintTemplate
        }
    }

    fun getPrivateTemplate(name: String, user: CbUser): ConstraintTemplate {
        val constraintTemplate = constraintTemplateRepository!!.findByNameInUser(name, user.userId)
        if (constraintTemplate == null) {
            throw NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, name))
        } else {
            return constraintTemplate
        }
    }

    private fun delete(constraintTemplate: ConstraintTemplate, user: CbUser) {
        LOGGER.debug("Deleting constraint template. {} - {}", *arrayOf(constraintTemplate.id, constraintTemplate.name))
        val clusters = clusterRepository!!.findAllClustersForConstraintTemplate(constraintTemplate.id)
        if (clusters.isEmpty()) {
            if (user.userId != constraintTemplate.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Constraint templates can only be deleted by account admins or owners.")
            }
            if (ResourceStatus.USER_MANAGED == constraintTemplate.status) {
                constraintTemplateRepository!!.delete(constraintTemplate)
            } else {
                constraintTemplate.status = ResourceStatus.DEFAULT_DELETED
                constraintTemplateRepository!!.save(constraintTemplate)
            }
        } else {
            if (isRunningClusterReferToTemplate(clusters)) {
                throw BadRequestException(String.format(
                        "There are stacks associated with template '%s'. Please remove these before deleting the template.", constraintTemplate.name))
            } else {
                val now = Date()
                val terminatedName = constraintTemplate.name + DELIMITER + now.time
                constraintTemplate.name = terminatedName
                constraintTemplate.isDeleted = true
                if (ResourceStatus.DEFAULT == constraintTemplate.status) {
                    constraintTemplate.status = ResourceStatus.DEFAULT_DELETED
                }
                constraintTemplateRepository!!.save(constraintTemplate)
            }
        }
    }

    private fun isRunningClusterReferToTemplate(clusters: List<Cluster>): Boolean {
        var result = false
        for (cluster in clusters) {
            if (!cluster.isDeleteCompleted) {
                result = true
                break
            }
        }
        return result
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ConstraintTemplateService::class.java)

        private val CONSTRAINT_NOT_FOUND_MSG = "Constraint template '%s' not found."
        private val DELIMITER = "_"
    }

}
