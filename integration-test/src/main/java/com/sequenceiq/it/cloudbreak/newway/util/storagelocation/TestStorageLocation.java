package com.sequenceiq.it.cloudbreak.newway.util.storagelocation;

abstract class TestStorageLocation {

    private final String baseStorageLocationName;

    private final String clusterName;

    TestStorageLocation(String baseStorageLocationName, String clusterName) {
        this.baseStorageLocationName = baseStorageLocationName;
        this.clusterName = clusterName;
    }

    String getBaseStorageLocationName() {
        return baseStorageLocationName;
    }

    String getClusterName() {
        return clusterName;
    }

    abstract String getStorageTypePrefix();

    abstract String getPathInfix();

}
