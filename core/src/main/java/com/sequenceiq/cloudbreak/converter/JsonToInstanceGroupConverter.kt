package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway

import javax.inject.Inject

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.service.template.TemplateService

@Component
class JsonToInstanceGroupConverter : AbstractConversionServiceAwareConverter<InstanceGroupJson, InstanceGroup>() {

    @Inject
    private val templateService: TemplateService? = null

    override fun convert(json: InstanceGroupJson): InstanceGroup {
        val instanceGroup = InstanceGroup()
        instanceGroup.groupName = json.group
        instanceGroup.nodeCount = json.nodeCount
        instanceGroup.instanceGroupType = json.type
        if (isGateway(instanceGroup.instanceGroupType) && instanceGroup.nodeCount !== instanceGroup.instanceGroupType.fixedNodeCount) {
            throw BadRequestException(String.format("Gateway has to be exactly %s node.", instanceGroup.instanceGroupType.fixedNodeCount))
        }
        try {
            instanceGroup.template = templateService!!.get(json.templateId)
        } catch (e: AccessDeniedException) {
            throw AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.templateId), e)
        }

        return instanceGroup
    }
}
