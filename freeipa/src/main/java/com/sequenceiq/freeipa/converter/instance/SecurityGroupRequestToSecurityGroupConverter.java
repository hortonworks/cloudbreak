package com.sequenceiq.freeipa.converter.instance;

import static java.util.Optional.ofNullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.converter.instance.securityrule.SecurityRuleRequestToSecurityRuleConverter;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;

@Component
public class SecurityGroupRequestToSecurityGroupConverter implements Converter<SecurityGroupRequest, SecurityGroup> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private SecurityRuleRequestToSecurityRuleConverter securityRuleConverter;

    @Override
    public SecurityGroup convert(@Nonnull SecurityGroupRequest source) {
        SecurityGroup entity = new SecurityGroup();
        entity.setName(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP));
        entity.setSecurityGroupIds(convertSecurityGroupIdsToList(source));
        entity.setSecurityRules(convertSecurityRules(source.getSecurityRules(), entity));
        return entity;
    }

    private Set<String> convertSecurityGroupIdsToList(SecurityGroupRequest source) {
        Set<String> securityGroupIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(source.getSecurityGroupIds())) {
            securityGroupIds.addAll(source.getSecurityGroupIds());
        }
        return securityGroupIds;
    }

    private Set<SecurityRule> convertSecurityRules(List<SecurityRuleRequest> securityRules, SecurityGroup securityGroup) {
        Set<SecurityRule> convertedSet = ofNullable(securityRules).orElse(List.of()).stream()
                .map(rule -> securityRuleConverter.convert(rule)).collect(Collectors.toSet());
        for (SecurityRule securityRule : convertedSet) {
            securityRule.setSecurityGroup(securityGroup);
        }
        return convertedSet;
    }
}
