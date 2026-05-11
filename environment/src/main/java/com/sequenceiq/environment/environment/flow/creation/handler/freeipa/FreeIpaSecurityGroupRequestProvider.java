package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;

@Service
public class FreeIpaSecurityGroupRequestProvider {

    @Inject
    private FreeIpaDefaultSecurityRuleRequestProvider defaultSecurityRuleRequestProvider;

    @Inject
    private FreeIpaHybridSecurityRuleRequestProvider hybridSecurityRuleRequestProvider;

    public SecurityGroupRequest createSecurityGroupRequest(EnvironmentDto environment) {
        EnvironmentType environmentType = environment.getEnvironmentType();
        SecurityAccessDto securityAccess = environment.getSecurityAccess();

        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        if (!Strings.isNullOrEmpty(securityAccess.getCidr())) {
            securityGroupRequest.setSecurityRules(new ArrayList<>());
            for (String cidr : CidrUtil.cidrs(securityAccess.getCidr())) {
                List<SecurityRuleRequest> securityRuleRequests = EnvironmentType.HYBRID.equals(environmentType)
                        ? hybridSecurityRuleRequestProvider.createSecurityRuleRequests(cidr)
                        : defaultSecurityRuleRequestProvider.createSecurityRuleRequests(cidr);
                securityGroupRequest.getSecurityRules().addAll(securityRuleRequests);
            }
            securityGroupRequest.setSecurityGroupIds(new HashSet<>());
        } else if (!Strings.isNullOrEmpty(securityAccess.getDefaultSecurityGroupId())) {
            securityGroupRequest.setSecurityGroupIds(getSecurityGroupIds(securityAccess.getDefaultSecurityGroupId()));
            securityGroupRequest.setSecurityRules(new ArrayList<>());
        } else {
            securityGroupRequest.setSecurityRules(new ArrayList<>());
            securityGroupRequest.setSecurityGroupIds(new HashSet<>());
        }
        return securityGroupRequest;
    }

}
