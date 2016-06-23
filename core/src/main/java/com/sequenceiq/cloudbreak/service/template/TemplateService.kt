package com.sequenceiq.cloudbreak.service.template

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
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.TemplateRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class TemplateService {

    @Inject
    private val templateRepository: TemplateRepository? = null

    @Inject
    private val stackRepository: StackRepository? = null

    fun retrievePrivateTemplates(user: CbUser): Set<Template> {
        return templateRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountTemplates(user: CbUser): Set<Template> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return templateRepository!!.findAllInAccount(user.account)
        } else {
            return templateRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Template {
        val template = templateRepository!!.findOne(id)
        if (template == null) {
            throw NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id))
        } else {
            return template
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, template: Template): Template {
        LOGGER.debug("Creating template: [User: '{}', Account: '{}']", user.username, user.account)
        var savedTemplate: Template? = null
        template.owner = user.userId
        template.account = user.account
        try {
            savedTemplate = templateRepository!!.save(template)
        } catch (ex: Exception) {
            throw DuplicateKeyValueException(APIResourceType.TEMPLATE, template.name, ex)
        }

        return savedTemplate
    }

    fun delete(templateId: Long?, user: CbUser) {
        val template = templateRepository!!.findByIdInAccount(templateId, user.account) ?: throw NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateId))
        delete(template, user)
    }

    fun getPrivateTemplate(name: String, user: CbUser): Template {
        val template = templateRepository!!.findByNameInUser(name, user.userId)
        if (template == null) {
            throw NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, name))
        } else {
            return template
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun getPublicTemplate(name: String, user: CbUser): Template {
        val template = templateRepository!!.findOneByName(name, user.account)
        if (template == null) {
            throw NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, name))
        } else {
            return template
        }
    }

    fun delete(templateName: String, user: CbUser) {
        val template = templateRepository!!.findByNameInAccount(templateName, user.account, user.userId) ?: throw NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateName))
        delete(template, user)
    }

    private fun delete(template: Template, user: CbUser) {
        LOGGER.debug("Deleting template. {} - {}", *arrayOf(template.id, template.name))
        val allStackForTemplate = stackRepository!!.findAllStackForTemplate(template.id)
        if (allStackForTemplate.isEmpty()) {
            if (user.userId != template.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Templates can be deleted only by account admins or owners.")
            }
            template.topology = null
            if (ResourceStatus.USER_MANAGED == template.status) {
                templateRepository!!.delete(template)
            } else {
                template.status = ResourceStatus.DEFAULT_DELETED
                templateRepository!!.save(template)
            }
        } else {
            if (isRunningStackReferToTemplate(allStackForTemplate)) {
                throw BadRequestException(String.format(
                        "There are stacks associated with template '%s'. Please remove these before deleting the template.", template.name))
            } else {
                val now = Date()
                val terminatedName = template.name + DELIMITER + now.time
                template.name = terminatedName
                template.topology = null
                template.isDeleted = true
                if (ResourceStatus.DEFAULT == template.status) {
                    template.status = ResourceStatus.DEFAULT_DELETED
                }
                templateRepository!!.save(template)
            }
        }
    }

    private fun isRunningStackReferToTemplate(allStackForTemplate: List<Stack>): Boolean {
        var result = false
        for (stack in allStackForTemplate) {
            if (!stack.isDeleteCompleted) {
                result = true
                break
            }
        }
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TemplateService::class.java)
        private val DELIMITER = "_"

        private val TEMPLATE_NOT_FOUND_MSG = "Template '%s' not found."
    }

}
