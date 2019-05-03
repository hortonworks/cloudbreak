package com.sequenceiq.freeipa.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;
import com.sequenceiq.freeipa.repository.SecurityRuleRepository;

@Service
public class SecurityRuleService {

    @Inject
    private SecurityRuleRepository securityRuleRepository;

    public List<SecurityRule> findAllBySecurityGroup(SecurityGroup securityGroup) {
        return securityRuleRepository.findAllBySecurityGroup(securityGroup);
    }

}
