package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityRuleEndpoint;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;

@Controller
@Transactional(TxType.NEVER)
public class SecurityRuleController implements SecurityRuleEndpoint {

    @Inject
    private SecurityRuleService securityRuleService;

    @Override
    public SecurityRulesResponse getDefaultSecurityRules(Boolean knoxEnabled) {
        return securityRuleService.getDefaultSecurityRules(knoxEnabled);
    }
}
