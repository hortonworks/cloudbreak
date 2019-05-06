package com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain;

import java.util.HashSet;
import java.util.Set;

public class SupportedVersions {

    private Set<SupportedVersion> supportedVersions = new HashSet<>();

    public Set<SupportedVersion> getSupportedVersions() {
        return supportedVersions;
    }

    public void setSupportedVersions(Set<SupportedVersion> supportedVersions) {
        this.supportedVersions = supportedVersions;
    }
}
