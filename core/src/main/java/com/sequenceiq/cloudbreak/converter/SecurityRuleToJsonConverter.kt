package com.sequenceiq.cloudbreak.converter

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.domain.SecurityRule

@Component
class SecurityRuleToJsonConverter : AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleJson>() {

    override fun convert(entity: SecurityRule): SecurityRuleJson {
        val json = SecurityRuleJson(entity.cidr)
        json.id = entity.id
        json.ports = StringUtils.join(entity.ports, ",")
        json.protocol = entity.protocol
        json.isModifiable = entity.isModifiable
        return json
    }
}
