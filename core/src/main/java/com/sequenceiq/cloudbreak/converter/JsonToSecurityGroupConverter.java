package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class JsonToSecurityGroupConverter extends AbstractConversionServiceAwareConverter<SecurityGroupRequest, SecurityGroup> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public SecurityGroup convert(SecurityGroupRequest source) {
        SecurityGroup entity = new SecurityGroup();
        if (Strings.isNullOrEmpty(source.getName())) {
            source.setName(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP));
        } else {
            entity.setName(source.getName());
        }
        entity.setDescription(source.getDescription());
        entity.setStatus(ResourceStatus.USER_MANAGED);
        entity.setSecurityGroupId(source.getSecurityGroupId());
        entity.setSecurityRules(convertSecurityRules(source.getSecurityRules(), entity));
        entity.setCloudPlatform(source.getCloudPlatform());
        return entity;
    }

    private Set<SecurityRule> convertSecurityRules(List<SecurityRuleRequest> securityRuleRequests, SecurityGroup securityGroup) {
        Set<SecurityRule> convertedSet = (Set<SecurityRule>) getConversionService().convert(securityRuleRequests, TypeDescriptor.forObject(securityRuleRequests),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(SecurityRule.class)));
        for (SecurityRule securityRule : convertedSet) {
            securityRule.setSecurityGroup(securityGroup);
        }
        return convertedSet;
    }
}
