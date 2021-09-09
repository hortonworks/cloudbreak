package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.securityrule.SecurityRuleToSecurityRuleV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;

@Component
public class SecurityGroupToSecurityGroupResponseConverter {

    @Inject
    private SecurityRuleToSecurityRuleV4ResponseConverter securityRuleToSecurityRuleV4ResponseConverter;

    public SecurityGroupV4Response convert(SecurityGroup source) {
        SecurityGroupV4Response json = new SecurityGroupV4Response();
        json.setSecurityRules(source.getSecurityRules().stream()
            .map(s -> securityRuleToSecurityRuleV4ResponseConverter.convert(s))
            .collect(Collectors.toList()));
        json.setSecurityGroupIds(source.getSecurityGroupIds());
        return json;
    }

}
