package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson
import com.sequenceiq.cloudbreak.domain.InstanceMetaData

@Component
class MetaDataToJsonConverter : AbstractConversionServiceAwareConverter<InstanceMetaData, InstanceMetaDataJson>() {

    override fun convert(entity: InstanceMetaData): InstanceMetaDataJson {
        val metaDataJson = InstanceMetaDataJson()
        metaDataJson.privateIp = entity.privateIp
        metaDataJson.publicIp = entity.publicIpWrapper
        metaDataJson.sshPort = entity.sshPort
        metaDataJson.ambariServer = entity.ambariServer
        metaDataJson.instanceId = entity.instanceId
        metaDataJson.discoveryFQDN = entity.discoveryFQDN
        metaDataJson.instanceGroup = entity.instanceGroup.groupName
        metaDataJson.instanceStatus = entity.instanceStatus
        return metaDataJson
    }
}
