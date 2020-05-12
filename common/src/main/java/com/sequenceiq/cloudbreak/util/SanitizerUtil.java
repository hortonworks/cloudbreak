package com.sequenceiq.cloudbreak.util;

import org.apache.commons.lang3.StringUtils;

public class SanitizerUtil {

    private SanitizerUtil() {
    }

    public static String sanitizeWorkloadUsername(String userName) {
        return StringUtils.substringBefore(userName, "@").toLowerCase().replaceAll("[^a-z0-9_]", "");
    }
}
