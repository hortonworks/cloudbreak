package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.domain.HostGroup

@Component
class JsonToHostGroupConverter : AbstractConversionServiceAwareConverter<HostGroupJson, HostGroup>() {
    override fun convert(source: HostGroupJson): HostGroup {
        val hostGroup = HostGroup()
        hostGroup.name = source.name
        return hostGroup
    }
}
