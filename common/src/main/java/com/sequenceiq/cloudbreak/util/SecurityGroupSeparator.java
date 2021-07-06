package com.sequenceiq.cloudbreak.util;

import java.util.Set;

import com.google.common.base.Strings;

public class SecurityGroupSeparator {

    private SecurityGroupSeparator() {
    }

    public static Set<String> getSecurityGroupIds(String securityGroup) {
        if (Strings.isNullOrEmpty(securityGroup)) {
            return Set.of();
        }
        return Set.of(securityGroup.split(","));
    }

}
