package com.sequenceiq.cloudbreak.service.image;

import java.util.Set;

public class PrefixMatchImages {

    private final Set<String> vMImageUUIDs;

    private final Set<String> defaultVMImageUUIDs;

    private final Set<String> supportedVersions;

    public PrefixMatchImages(Set<String> vMImageUUIDs, Set<String> defaultVMImageUUIDs, Set<String> supportedVersions) {
        this.vMImageUUIDs = vMImageUUIDs;
        this.defaultVMImageUUIDs = defaultVMImageUUIDs;
        this.supportedVersions = supportedVersions;
    }

    public Set<String> getvMImageUUIDs() {
        return vMImageUUIDs;
    }

    public Set<String> getDefaultVMImageUUIDs() {
        return defaultVMImageUUIDs;
    }

    public Set<String> getSupportedVersions() {
        return supportedVersions;
    }
}
