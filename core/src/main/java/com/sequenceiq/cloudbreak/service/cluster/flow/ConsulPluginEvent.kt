package com.sequenceiq.cloudbreak.service.cluster.flow

enum class ConsulPluginEvent private constructor(val name: String) {

    PRE_INSTALL("recipe-pre-install"),
    POST_INSTALL("recipe-post-install"),
    START_AMBARI_EVENT("ambari-start"),
    STOP_AMBARI_EVENT("ambari-stop"),
    RESTART_AMBARI_EVENT("ambari-restart"),
    RESET_AMBARI_DB_EVENT("ambari-db-reset"),
    RESET_AMBARI_EVENT("ambari-reset"),
    SSSD_SETUP("sssd-setup")
}
