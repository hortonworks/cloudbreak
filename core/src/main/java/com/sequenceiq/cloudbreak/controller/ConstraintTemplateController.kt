package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable

import com.sequenceiq.cloudbreak.api.endpoint.ConstraintTemplateEndpoint
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.constraint.ConstraintTemplateService

@Component
class ConstraintTemplateController : ConstraintTemplateEndpoint {
    @Autowired
    private val constraintTemplateService: ConstraintTemplateService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun postPrivate(constraintTemplateRequest: ConstraintTemplateRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createConstraintTemplate(user, constraintTemplateRequest, false)
    }

    override fun postPublic(constraintTemplateRequest: ConstraintTemplateRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createConstraintTemplate(user, constraintTemplateRequest, true)
    }

    override fun getPrivates(): Set<ConstraintTemplateResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val constraintTemplates = constraintTemplateService!!.retrievePrivateConstraintTemplates(user)
        return convert(constraintTemplates)
    }

    override fun getPublics(): Set<ConstraintTemplateResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val templates = constraintTemplateService!!.retrieveAccountConstraintTemplates(user)
        return convert(templates)
    }

    override fun get(id: Long?): ConstraintTemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val template = constraintTemplateService!!.get(id)
        return convert(template)
    }

    override fun getPrivate(name: String): ConstraintTemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val template = constraintTemplateService!!.getPrivateTemplate(name, user)
        return convert(template)
    }

    override fun getPublic(@PathVariable name: String): ConstraintTemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val template = constraintTemplateService!!.getPublicTemplate(name, user)
        return convert(template)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        constraintTemplateService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        constraintTemplateService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        constraintTemplateService!!.delete(name, user)
    }

    private fun createConstraintTemplate(user: CbUser, constraintTemplateRequest: ConstraintTemplateRequest, publicInAccount: Boolean): IdJson {
        var constraintTemplate = convert(constraintTemplateRequest, publicInAccount)
        constraintTemplate = constraintTemplateService!!.create(user, constraintTemplate)
        return IdJson(constraintTemplate.id)
    }

    private fun convert(constraintTemplateRequest: ConstraintTemplateRequest, publicInAccount: Boolean): ConstraintTemplate {
        val converted = conversionService!!.convert<ConstraintTemplate>(constraintTemplateRequest, ConstraintTemplate::class.java)
        converted.isPublicInAccount = publicInAccount
        return converted
    }

    private fun convert(constraintTemplate: ConstraintTemplate): ConstraintTemplateResponse {
        return conversionService!!.convert<ConstraintTemplateResponse>(constraintTemplate, ConstraintTemplateResponse::class.java)
    }

    private fun convert(constraintTemplates: Set<ConstraintTemplate>): Set<ConstraintTemplateResponse> {
        val jsons = HashSet<ConstraintTemplateResponse>()
        for (constraintTemplate in constraintTemplates) {
            jsons.add(convert(constraintTemplate))
        }
        return jsons
    }

}
