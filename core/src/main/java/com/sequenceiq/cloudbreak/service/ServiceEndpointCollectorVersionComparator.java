package com.sequenceiq.cloudbreak.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;

@Service
public class ServiceEndpointCollectorVersionComparator {

    private VersionComparator versionComparator = new VersionComparator();

    public boolean minVersionSupported(Optional<String> blueprintVersionOptional, String minVersionString) {
        if (Strings.isNullOrEmpty(minVersionString)) {
            return true;
        } else if (blueprintVersionOptional.isEmpty()) {
            return true;
        } else {
            Versioned blueprintVersion = () -> StringUtils.substringBefore(blueprintVersionOptional.get(), "-");
            Versioned minVersion = () -> StringUtils.substringBefore(minVersionString, "-");
            return versionComparator.compare(blueprintVersion, minVersion) >= 0;
        }
    }

    public boolean maxVersionSupported(Optional<String> blueprintVersionOptional, String maxVersionString) {
        if (Strings.isNullOrEmpty(maxVersionString)) {
            return true;
        } else if (blueprintVersionOptional.isEmpty()) {
            return true;
        } else {
            Versioned blueprintVersion = () -> StringUtils.substringBefore(blueprintVersionOptional.get(), "-");
            Versioned maxVersion = () -> StringUtils.substringBefore(maxVersionString, "-");
            return versionComparator.compare(blueprintVersion, maxVersion) <= 0;
        }
    }
}
