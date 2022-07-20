package com.sequenceiq.cloudbreak.cluster.model;

/**
 * Parcel info received from the CM server is presented in this class
 */
public class ParcelInfo {

    private final String name;

    private final String version;

    private final ParcelStatus status;

    public ParcelInfo(String name, String version, ParcelStatus status) {
        this.name = name;
        this.version = version;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ParcelStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ParcelInfo{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
