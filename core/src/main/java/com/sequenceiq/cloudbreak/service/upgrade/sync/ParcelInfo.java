package com.sequenceiq.cloudbreak.service.upgrade.sync;

/**
 * Parcel info received from the CM server is presented in this class
 */
class ParcelInfo {

    private final String name;

    private final String version;

    ParcelInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    String getName() {
        return name;
    }

    String getVersion() {
        return version;
    }
}
