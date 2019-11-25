package com.sequenceiq.cloudbreak.util;

import com.google.common.base.Strings;

public class CidrUtil {

    private CidrUtil() {

    }

    public static String[] cidrs(String cidr) {
        return Strings.isNullOrEmpty(cidr) ? new String[]{} : cidr.split(",");
    }
}
