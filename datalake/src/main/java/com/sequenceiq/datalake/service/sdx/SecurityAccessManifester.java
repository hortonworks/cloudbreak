package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.SecurityRuleUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.util.CidrUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class SecurityAccessManifester {

    @Inject
    private InternalApiCallCalculator internalApiCallCalculator;

    public void overrideSecurityAccess(InstanceGroupType instanceGroupType, List<InstanceGroupV4Request> instanceGroups, String securityGroupId, String cidrs) {
        instanceGroups.stream()
                .filter(ig -> ig.getType() == instanceGroupType)
                .findFirst()
                .ifPresent(ig -> {
                    SecurityGroupV4Request securityGroup = ig.getSecurityGroup();
                    if (securityGroup == null) {
                        securityGroup = new SecurityGroupV4Request();
                    }
                    if (!internalApiCallCalculator.isInternalApiCall(securityGroup)) {
                        if (!Strings.isNullOrEmpty(securityGroupId)) {
                            securityGroup.setSecurityGroupIds(Set.of(securityGroupId));
                            securityGroup.setSecurityRules(new ArrayList<>());
                        } else if (!Strings.isNullOrEmpty(cidrs)) {
                            List<SecurityRuleV4Request> generatedSecurityRules = new ArrayList<>();
                            List<SecurityRuleV4Request> originalSecurityRules = securityGroup.getSecurityRules();
                            for (String cidr : CidrUtil.cidrs(cidrs)) {
                                SecurityRuleUtil.propagateCidr(generatedSecurityRules, originalSecurityRules, cidr);
                            }
                            // Because of YCLOUD we should not set this if null
                            if (originalSecurityRules != null) {
                                securityGroup.setSecurityRules(generatedSecurityRules);
                            }
                            securityGroup.setSecurityGroupIds(new HashSet<>());
                        } else {
                            securityGroup.setSecurityGroupIds(new HashSet<>());
                            securityGroup.setSecurityRules(new ArrayList<>());
                        }
                    }
                });
    }
}
