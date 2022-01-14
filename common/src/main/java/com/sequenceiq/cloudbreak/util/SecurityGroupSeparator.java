package com.sequenceiq.cloudbreak.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

public class SecurityGroupSeparator {

    private SecurityGroupSeparator() {
    }

    public static Set<String> getSecurityGroupIds(String securityGroup) {
        if (Strings.isNullOrEmpty(securityGroup)) {
            return Set.of();
        }
        return Arrays.stream(securityGroup.split(","))
                .map(e -> e.trim())
                .collect(Collectors.toSet());
    }

}
