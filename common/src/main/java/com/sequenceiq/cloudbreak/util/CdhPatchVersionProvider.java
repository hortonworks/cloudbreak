package com.sequenceiq.cloudbreak.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CdhPatchVersionProvider {

    private static final String PATCH_VERSION_REGEX = "\\.p([0-9]+)\\.";

    public Optional<Integer> getPatchFromVersionString(String version) {
        if (StringUtils.hasText(version)) {
            Matcher matcher = Pattern.compile(PATCH_VERSION_REGEX).matcher(version);
            return matcher.find() ? Optional.of(Integer.valueOf(matcher.group(1))) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }
}
