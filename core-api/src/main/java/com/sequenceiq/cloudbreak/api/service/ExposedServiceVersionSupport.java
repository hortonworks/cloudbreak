package com.sequenceiq.cloudbreak.api.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Service
public class ExposedServiceVersionSupport {

    private final VersionComparator versionComparator = new VersionComparator();

    public boolean minVersionSupported(Optional<String> blueprintVersionOptional, String minVersionString) {
        boolean shouldInclude;
        if (Strings.isNullOrEmpty(minVersionString)) {
            shouldInclude = true;
        } else if (blueprintVersionOptional.isEmpty()) {
            shouldInclude = true;
        } else {
            Versioned blueprintVersion = () -> StringUtils.substringBefore(blueprintVersionOptional.get(), "-");
            Versioned minVersion = () -> StringUtils.substringBefore(minVersionString, "-");
            shouldInclude = versionComparator.compare(blueprintVersion, minVersion) >= 0;
        }
        return shouldInclude;
    }

    public boolean maxVersionSupported(Optional<String> blueprintVersionOptional, String maxVersionString) {
        boolean shouldInclude;
        if (Strings.isNullOrEmpty(maxVersionString)) {
            shouldInclude = true;
        } else if (blueprintVersionOptional.isEmpty()) {
            shouldInclude = true;
        } else {
            Versioned blueprintVersion = () -> StringUtils.substringBefore(blueprintVersionOptional.get(), "-");
            Versioned maxVersion = () -> StringUtils.substringBefore(maxVersionString, "-");
            shouldInclude = versionComparator.compare(blueprintVersion, maxVersion) <= 0;
        }
        return shouldInclude;
    }
}
