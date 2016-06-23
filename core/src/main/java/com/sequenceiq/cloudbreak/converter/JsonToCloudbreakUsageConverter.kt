package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson

@Component
class JsonToCloudbreakUsageConverter : AbstractConversionServiceAwareConverter<CloudbreakUsageJson, CloudbreakUsage>() {
    override fun convert(json: CloudbreakUsageJson): CloudbreakUsage {
        val entity = CloudbreakUsage()
        entity.owner = json.owner
        entity.account = json.account
        entity.provider = json.provider
        entity.region = json.region
        entity.availabilityZone = json.availabilityZone
        entity.instanceHours = json.instanceHours
        entity.stackId = json.stackId
        entity.stackName = json.stackName
        entity.instanceType = json.instanceType
        entity.instanceGroup = json.instanceGroup
        return entity
    }
}
