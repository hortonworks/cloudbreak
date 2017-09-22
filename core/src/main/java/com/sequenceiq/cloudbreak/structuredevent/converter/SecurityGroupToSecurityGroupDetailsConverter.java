package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityRuleDetails;

@Component
public class SecurityGroupToSecurityGroupDetailsConverter extends AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupDetails> {
    @Inject
    private ConversionService conversionService;

    @Override
    public SecurityGroupDetails convert(SecurityGroup source) {
        SecurityGroupDetails securityGroupDetails = new SecurityGroupDetails();
        securityGroupDetails.setId(source.getId());
        securityGroupDetails.setName(source.getName());
        securityGroupDetails.setDescription(source.getDescription());
        securityGroupDetails.setSecurityGroupId(source.getSecurityGroupId());
        securityGroupDetails.setSecurityRules((Set<SecurityRuleDetails>) conversionService.convert(source.getSecurityRules(),
                TypeDescriptor.forObject(source.getSecurityRules()),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(SecurityRuleDetails.class))));
        return securityGroupDetails;
    }
}
