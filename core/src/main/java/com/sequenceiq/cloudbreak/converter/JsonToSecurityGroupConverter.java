package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class JsonToSecurityGroupConverter extends AbstractConversionServiceAwareConverter<SecurityGroupJson, SecurityGroup> {

    @Override
    public SecurityGroup convert(SecurityGroupJson source) {
        SecurityGroup entity = new SecurityGroup();
        entity.setName(source.getName());
        entity.setDescription(source.getDescription());
        entity.setStatus(ResourceStatus.USER_MANAGED);
        entity.setSecurityGroupId(source.getSecurityGroupId());
        entity.setSecurityRules(convertSecurityRules(source.getSecurityRules(), entity));
        entity.setCloudPlatform(source.getCloudPlatform());
        return entity;
    }

    private Set<SecurityRule> convertSecurityRules(List<SecurityRuleJson> securityRuleJsons, SecurityGroup securityGroup) {
        Set<SecurityRule> convertedSet = (Set<SecurityRule>) getConversionService().convert(securityRuleJsons, TypeDescriptor.forObject(securityRuleJsons),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(SecurityRule.class)));
        for (SecurityRule securityRule : convertedSet) {
            securityRule.setSecurityGroup(securityGroup);
        }
        return convertedSet;
    }
}
