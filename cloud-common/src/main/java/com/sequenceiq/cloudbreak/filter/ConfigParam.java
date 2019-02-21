package com.sequenceiq.cloudbreak.filter;

public enum ConfigParam {

    YARN_RM_WEB_ADDRESS("yarn.resourcemanager.webapp.address"),
    NAMENODE_HTTP_ADDRESS("dfs.namenode.http-address"),
    SECONDARY_NAMENODE_HTTP_ADDRESS("dfs.namenode.secondary.http-address"),
    DFS_REPLICATION("dfs.replication");

    private final String key;

    ConfigParam(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

}
