package com.sequenceiq.cloudbreak.service.cluster

enum class ConfigParam private constructor(private val key: String) {

    YARN_RM_WEB_ADDRESS("yarn.resourcemanager.webapp.address"),
    NAMENODE_HTTP_ADDRESS("dfs.namenode.http-address"),
    SECONDARY_NAMENODE_HTTP_ADDRESS("dfs.namenode.secondary.http-address"),
    DFS_REPLICATION("dfs.replication");

    fun key(): String {
        return key
    }

}
