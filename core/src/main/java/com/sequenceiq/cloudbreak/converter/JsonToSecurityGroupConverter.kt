package com.sequenceiq.cloudbreak.converter

import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.domain.SecurityRule

@Component
class JsonToSecurityGroupConverter : AbstractConversionServiceAwareConverter<SecurityGroupJson, SecurityGroup>() {

    override fun convert(source: SecurityGroupJson): SecurityGroup {
        val entity = SecurityGroup()
        entity.name = source.name
        entity.description = source.description
        entity.status = ResourceStatus.USER_MANAGED

        entity.securityRules = convertSecurityRules(source.securityRules, entity)
        return entity
    }

    private fun convertSecurityRules(securityRuleJsons: List<SecurityRuleJson>, securityGroup: SecurityGroup): Set<SecurityRule> {
        val convertedSet = conversionService.convert(securityRuleJsons, TypeDescriptor.forObject(securityRuleJsons),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(SecurityRule::class.java))) as Set<SecurityRule>
        for (securityRule in convertedSet) {
            securityRule.securityGroup = securityGroup
        }
        return convertedSet
    }
}
