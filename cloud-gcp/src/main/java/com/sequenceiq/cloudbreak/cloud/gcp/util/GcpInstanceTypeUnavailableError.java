package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public enum GcpInstanceTypeUnavailableError {

    ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS",
            List.of(Pattern.compile("VM instance.* is currently unavailable in the .*zone", Pattern.CASE_INSENSITIVE))),

    ZONE_RESOURCE_POOL_EXHAUSTED("ZONE_RESOURCE_POOL_EXHAUSTED",
            List.of(Pattern.compile("does not have enough resources available to fulfill the request", Pattern.CASE_INSENSITIVE))),

    INVALID_MACHINE_TYPE("INVALID_MACHINE_TYPE",
            List.of(Pattern.compile(Pattern.quote("Invalid value for field 'resource.machineType'"), Pattern.CASE_INSENSITIVE)));

    private final String code;

    private final List<Pattern> messagePatterns;

    GcpInstanceTypeUnavailableError(String code, List<Pattern> messagePatterns) {
        this.code = code;
        this.messagePatterns = messagePatterns;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String text) {
        return text != null
                && (text.toUpperCase(Locale.ROOT).contains(code) || messagePatterns.stream().anyMatch(pattern -> pattern.matcher(text).find()));
    }
}
