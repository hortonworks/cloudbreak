package com.sequenceiq.cloudbreak.converter

import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.domain.SecurityRule

@Component
class SecurityGroupToJsonConverter : AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupJson>() {

    override fun convert(source: SecurityGroup): SecurityGroupJson {
        val json = SecurityGroupJson()
        json.id = source.id
        json.name = source.name
        json.description = source.description
        json.account = source.account
        json.owner = source.owner
        json.isPublicInAccount = source.isPublicInAccount
        json.securityRules = convertSecurityRules(source.securityRules)
        return json
    }

    private fun convertSecurityRules(securityRules: Set<SecurityRule>): List<SecurityRuleJson> {
        return conversionService.convert(securityRules, TypeDescriptor.forObject(securityRules),
                TypeDescriptor.collection(List<Any>::class.java, TypeDescriptor.valueOf(SecurityRuleJson::class.java))) as List<SecurityRuleJson>
    }
}
