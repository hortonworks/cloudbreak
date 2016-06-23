package com.sequenceiq.cloudbreak.service.template

import java.util.HashSet

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.cloudbreak.repository.TemplateRepository
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

@Service
@Transactional
class DefaultTemplateLoaderService {

    @Value("#{'${cb.template.defaults:}'.split(',')}")
    private val templateArray: List<String>? = null

    @Inject
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Inject
    private val templateRepository: TemplateRepository? = null

    @Inject
    private val templateService: TemplateService? = null

    @Inject
    private val jsonHelper: JsonHelper? = null

    fun loadTemplates(user: CbUser): Set<Template> {
        val templates = HashSet<Template>()
        if (templateRepository!!.findAllDefaultInAccount(user.account).isEmpty()) {
            templates.addAll(createDefaultTemplates(user))
        }
        return templates
    }

    private fun createDefaultTemplates(user: CbUser): Set<Template> {
        val templates = HashSet<Template>()
        for (templateName in templateArray!!) {
            if (!templateName.isEmpty() && templateRepository!!.findOneByName(templateName, user.account) == null) {
                try {
                    val jsonNode = jsonHelper!!.createJsonFromString(
                            FileReaderUtils.readFileFromClasspath(String.format("defaults/templates/%s.tmpl", templateName)))
                    val templateRequest = JsonUtil.treeToValue<TemplateRequest>(jsonNode, TemplateRequest::class.java)
                    val converted = conversionService!!.convert<Template>(templateRequest, Template::class.java)
                    converted.account = user.account
                    converted.owner = user.userId
                    converted.isPublicInAccount = true
                    converted.status = ResourceStatus.DEFAULT
                    templateRepository.save(converted)
                    templates.add(converted)
                } catch (e: Exception) {
                    LOGGER.error("Template is not available for '{}' user.", e, user)
                }

            }
        }
        return templates
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DefaultTemplateLoaderService::class.java)
    }

}
