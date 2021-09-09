package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityGroupDetails;

@Component
public class SecurityGroupToSecurityGroupDetailsConverter {

    @Inject
    private SecurityRuleToSecurityRuleDetailsConverter securityRuleToSecurityRuleDetailsConverter;

    public SecurityGroupDetails convert(SecurityGroup source) {
        SecurityGroupDetails securityGroupDetails = new SecurityGroupDetails();
        securityGroupDetails.setId(source.getId());
        securityGroupDetails.setName(source.getName());
        securityGroupDetails.setDescription(source.getDescription());
        securityGroupDetails.setSecurityGroupId(source.getFirstSecurityGroupId());
        securityGroupDetails.setSecurityGroupIds(source.getSecurityGroupIds());
        securityGroupDetails.setSecurityRules(
                source.getSecurityRules().stream()
                .map(rule -> securityRuleToSecurityRuleDetailsConverter.convert(rule))
                .collect(Collectors.toList())
        );
        return securityGroupDetails;
    }
}
