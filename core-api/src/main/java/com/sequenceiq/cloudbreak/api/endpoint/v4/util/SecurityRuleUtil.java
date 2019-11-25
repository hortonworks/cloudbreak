package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;

public class SecurityRuleUtil {

    private SecurityRuleUtil() {

    }

    public static void propagateCidr(List<SecurityRuleV4Request> generatedSecurityRules,
        List<SecurityRuleV4Request> originalSecurityRules,
        String cidr) {
        if (originalSecurityRules != null) {
            List<SecurityRuleV4Request> securityRules = originalSecurityRules
                    .stream()
                    .map(a -> SerializationUtils.clone(a))
                    .collect(Collectors.toList());
            securityRules.forEach(sr -> sr.setSubnet(cidr));
            generatedSecurityRules.addAll(securityRules);
        }
    }
}
