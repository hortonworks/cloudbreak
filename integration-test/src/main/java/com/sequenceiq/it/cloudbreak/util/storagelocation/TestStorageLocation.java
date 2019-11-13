package com.sequenceiq.it.cloudbreak.util.storagelocation;

abstract class TestStorageLocation {

    private final String baseStorageLocation;

    private final String clusterName;

    TestStorageLocation(String baseStorageLocation, String clusterName) {
        this.baseStorageLocation = baseStorageLocation;
        this.clusterName = clusterName;
    }

    String getBaseStorageLocation() {
        return baseStorageLocation;
    }

    String getClusterName() {
        return clusterName;
    }

    abstract String getStorageTypePrefix();

    abstract String getPathInfix();

}
