package com.sequenceiq.freeipa.converter.instance;

import jakarta.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupResponse;
import com.sequenceiq.freeipa.converter.instance.securityrule.SecurityRuleToSecurityRuleResponseConverter;
import com.sequenceiq.freeipa.entity.SecurityGroup;

@Component
public class SecurityGroupToSecurityGroupResponseConverter implements Converter<SecurityGroup, SecurityGroupResponse> {

    @Inject
    private SecurityRuleToSecurityRuleResponseConverter securityRuleResponseConverter;

    @Override
    public SecurityGroupResponse convert(SecurityGroup source) {
        SecurityGroupResponse json = new SecurityGroupResponse();
        json.setSecurityRules(securityRuleResponseConverter.convert(source.getSecurityRules()));
        json.setSecurityGroupIds(source.getSecurityGroupIds());
        return json;
    }

}
