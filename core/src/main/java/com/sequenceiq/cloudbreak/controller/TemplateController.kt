package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable

import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.template.DefaultTemplateLoaderService
import com.sequenceiq.cloudbreak.service.template.TemplateService

@Component
class TemplateController : TemplateEndpoint {
    @Autowired
    private val templateService: TemplateService? = null

    @Autowired
    private val defaultTemplateLoaderService: DefaultTemplateLoaderService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    private val templateValidator: TemplateValidator? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun postPrivate(templateRequest: TemplateRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        templateValidator!!.validateTemplateRequest(templateRequest)
        return createTemplate(user, templateRequest, false)
    }

    override fun postPublic(templateRequest: TemplateRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        templateValidator!!.validateTemplateRequest(templateRequest)
        return createTemplate(user, templateRequest, true)
    }

    override fun getPrivates(): Set<TemplateResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        defaultTemplateLoaderService!!.loadTemplates(user)
        val templates = templateService!!.retrievePrivateTemplates(user)
        return convert(templates)
    }

    override fun getPublics(): Set<TemplateResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        defaultTemplateLoaderService!!.loadTemplates(user)
        val templates = templateService!!.retrieveAccountTemplates(user)
        return convert(templates)
    }

    override fun get(id: Long?): TemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val template = templateService!!.get(id)
        return convert(template)
    }

    override fun getPrivate(name: String): TemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val template = templateService!!.getPrivateTemplate(name, user)
        return convert(template)
    }

    override fun getPublic(@PathVariable name: String): TemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val template = templateService!!.getPublicTemplate(name, user)
        return convert(template)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        templateService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        templateService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        templateService!!.delete(name, user)
    }

    private fun createTemplate(user: CbUser, templateRequest: TemplateRequest, publicInAccount: Boolean): IdJson {
        var template = convert(templateRequest, publicInAccount)
        template = templateService!!.create(user, template)
        return IdJson(template.id)
    }

    private fun convert(templateRequest: TemplateRequest, publicInAccount: Boolean): Template {
        val converted = conversionService!!.convert<Template>(templateRequest, Template::class.java)
        converted.isPublicInAccount = publicInAccount
        return converted
    }

    private fun convert(template: Template): TemplateResponse {
        return conversionService!!.convert<TemplateResponse>(template, TemplateResponse::class.java)
    }

    private fun convert(templates: Set<Template>): Set<TemplateResponse> {
        val jsons = HashSet<TemplateResponse>()
        for (template in templates) {
            jsons.add(convert(template))
        }
        return jsons
    }

}
