package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.securityrule.SecurityRuleV4RequestToSecurityRuleConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityGroupV4RequestToSecurityGroupConverter {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private SecurityRuleV4RequestToSecurityRuleConverter securityRuleV4RequestToSecurityRuleConverter;

    public SecurityGroup convert(@Nonnull SecurityGroupV4Request source) {
        SecurityGroup entity = new SecurityGroup();
        entity.setName(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP));
        entity.setStatus(ResourceStatus.USER_MANAGED);
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
        if (!CollectionUtils.isEmpty(securityRules)) {
            Set<SecurityRule> convertedSet = securityRules.stream()
                    .map(s -> securityRuleV4RequestToSecurityRuleConverter.convert(s))
                    .collect(Collectors.toSet());
            for (SecurityRule securityRule : convertedSet) {
                securityRule.setSecurityGroup(securityGroup);
            }
            return convertedSet;
        }
        return new HashSet<>();
    }
}
