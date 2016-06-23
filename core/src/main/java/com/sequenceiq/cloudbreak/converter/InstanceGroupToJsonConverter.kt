package com.sequenceiq.cloudbreak.converter

import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData

@Component
class InstanceGroupToJsonConverter : AbstractConversionServiceAwareConverter<InstanceGroup, InstanceGroupJson>() {

    override fun convert(entity: InstanceGroup): InstanceGroupJson {
        val instanceGroupJson = InstanceGroupJson()
        instanceGroupJson.group = entity.groupName
        instanceGroupJson.id = entity.id
        instanceGroupJson.nodeCount = entity.nodeCount!!
        instanceGroupJson.templateId = entity.template.id
        instanceGroupJson.type = entity.instanceGroupType
        instanceGroupJson.metadata = convertEntitiesToJson(entity.instanceMetaData)
        return instanceGroupJson
    }

    private fun convertEntitiesToJson(metadata: Set<InstanceMetaData>): Set<InstanceMetaDataJson> {
        return conversionService.convert(metadata,
                TypeDescriptor.forObject(metadata),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(InstanceMetaDataJson::class.java))) as Set<InstanceMetaDataJson>
    }

}
