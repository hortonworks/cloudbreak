package com.sequenceiq.cloudbreak.controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityRuleEndpoint;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SecurityRuleController implements SecurityRuleEndpoint {

    @Inject
    private SecurityRuleService securityRuleService;

    @Override
    public SecurityRulesResponse getDefaultSecurityRules() {
        return securityRuleService.getDefaultSecurityRules();
    }
}
