package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class VersionString implements Versioned {

    private final String version;

    VersionString(String version) {
        this.version = version;
    }

    @Override
    public String getVersion() {
        return version;
    }

}
