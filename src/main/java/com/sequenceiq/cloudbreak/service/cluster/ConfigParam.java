package com.sequenceiq.cloudbreak.service.cluster;

public enum ConfigParam {

    YARN_RM_WEB_ADDRESS("yarn.resourcemanager.webapp.address");

    private final String key;

    private ConfigParam(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

}
