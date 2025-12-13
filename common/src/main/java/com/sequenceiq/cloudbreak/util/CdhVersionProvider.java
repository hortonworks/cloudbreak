package com.sequenceiq.cloudbreak.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class CdhVersionProvider {

    private static final String STACK_VERSION_GROUP = "stackVersion";

    private static final String PATCH_VERSION_GROUP = "patchVersion";

    private static final String BUILD_NUMBER_GROUP = "buildNumber";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<" + STACK_VERSION_GROUP + ">\\d+\\.\\d+\\.\\d+)-?(?:.*p)?(?<" + PATCH_VERSION_GROUP + ">\\d+)?(?:\\.(?<" + BUILD_NUMBER_GROUP + ">\\d+))?");

    private CdhVersionProvider() {
    }

    public static Optional<String> getCdhStackVersionFromVersionString(String version) {
        if (StringUtils.hasText(version)) {
            Matcher matcher = PATTERN.matcher(version);
            return matcher.find()
                    ? Optional.ofNullable(matcher.group(STACK_VERSION_GROUP))
                    : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Integer> getCdhPatchVersionFromVersionString(String version) {
        if (StringUtils.hasText(version)) {
            Matcher matcher = PATTERN.matcher(version);
            return matcher.find()
                    ? Optional.ofNullable(matcher.group(PATCH_VERSION_GROUP) != null ? Integer.valueOf(matcher.group(PATCH_VERSION_GROUP)) : null)
                    : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Integer> getCdhBuildNumberFromVersionString(String version) {
        if (StringUtils.hasText(version)) {
            Matcher matcher = PATTERN.matcher(version);
            return matcher.find()
                    ? Optional.ofNullable(matcher.group(BUILD_NUMBER_GROUP) != null ? Integer.valueOf(matcher.group(BUILD_NUMBER_GROUP)) : null)
                    : Optional.empty();
        } else {
            return Optional.empty();
        }
    }
}
