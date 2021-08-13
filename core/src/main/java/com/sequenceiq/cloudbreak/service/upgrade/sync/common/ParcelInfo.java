package com.sequenceiq.cloudbreak.service.upgrade.sync.common;

/**
 * Parcel info received from the CM server is presented in this class
 */
public class ParcelInfo {

    private final String name;

    private final String version;

    public ParcelInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ParcelInfo{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
