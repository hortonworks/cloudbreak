package com.sequenceiq.freeipa.converter.instance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.freeipa.converter.instance.securityrule.SecurityRuleV4RequestToSecurityRuleConverter;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;
import com.sequenceiq.freeipa.service.MissingResourceNameGenerator;

@Component
public class SecurityGroupV4RequestToSecurityGroupConverter implements Converter<SecurityGroupV4Request, SecurityGroup> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private SecurityRuleV4RequestToSecurityRuleConverter securityRuleConverter;

    @Override
    public SecurityGroup convert(@Nonnull SecurityGroupV4Request source) {
        SecurityGroup entity = new SecurityGroup();
        entity.setName(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP));
        entity.setSecurityGroupIds(convertSecurityGroupIdsToList(source));
        entity.setSecurityRules(convertSecurityRules(source.getSecurityRules(), entity));
        return entity;
    }

    private Set<String> convertSecurityGroupIdsToList(SecurityGroupV4Request source) {
        Set<String> securityGroupIds = new HashSet<>();
        if (source.getSecurityGroupIds() != null && !source.getSecurityGroupIds().isEmpty()) {
            securityGroupIds.addAll(source.getSecurityGroupIds());
        }
        return securityGroupIds;
    }

    private Set<SecurityRule> convertSecurityRules(List<SecurityRuleV4Request> securityRules, SecurityGroup securityGroup) {
        Set<SecurityRule> convertedSet = securityRules.stream().map(rule -> securityRuleConverter.convert(rule)).collect(Collectors.toSet());
        for (SecurityRule securityRule : convertedSet) {
            securityRule.setSecurityGroup(securityGroup);
        }
        return convertedSet;
    }
}
