package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.domain.SecurityRule

@Component
class JsonToSecurityRuleConverter : AbstractConversionServiceAwareConverter<SecurityRuleJson, SecurityRule>() {
    override fun convert(json: SecurityRuleJson): SecurityRule {
        val entity = SecurityRule()
        entity.cidr = json.subnet
        entity.setPorts(json.ports)
        entity.protocol = json.protocol
        entity.isModifiable = json.isModifiable
        return entity
    }
}
