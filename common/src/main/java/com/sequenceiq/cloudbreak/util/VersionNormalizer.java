package com.sequenceiq.cloudbreak.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionNormalizer {
    private static final Pattern CDH_BASE_VERSION_PATTERN =
            Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private VersionNormalizer() {
    }

    public static String normalizeCdhVersion(String version) {
        Matcher matcher = CDH_BASE_VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group();
        }
        return version;
    }

}
