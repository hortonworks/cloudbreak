package com.sequenceiq.datalake.service.sdx.attach;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Provides a central logic point for generation of a detached SDX cluster name, as
 * well as reversing a detached name.
 */
@Component
public class SdxDetachNameGenerator {
    private static final Pattern DETACHED_NAME_PATTERN = Pattern.compile(".*(-[0-9]+)$");

    private static final String DELIMITER = "-";

    public String generateDetachedClusterName(String originalName) {
        return originalName + DELIMITER + new Date().getTime();
    }

    public String generateOriginalNameFromDetached(String detachedName) {
        Matcher detachedNameMatcher = DETACHED_NAME_PATTERN.matcher(detachedName);
        if (detachedNameMatcher.matches()) {
            return detachedName.substring(0, detachedName.lastIndexOf(DELIMITER));
        } else {
            throw new SdxDetachException(String.format(
                    "Provided detached name '%s' for generateOriginalNameFromDetached does not have expected pattern: '%s'!",
                    detachedName, DETACHED_NAME_PATTERN.pattern()
            ));
        }
    }
}
