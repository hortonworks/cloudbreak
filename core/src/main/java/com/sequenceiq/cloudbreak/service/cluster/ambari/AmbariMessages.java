package com.sequenceiq.cloudbreak.service.cluster.ambari;

public enum AmbariMessages {

    AMBARI_CLUSTER_RESETTING_AMBARI_DATABASE("ambari.cluster.resetting.ambari.database"),
    AMBARI_CLUSTER_AMBARI_DATABASE_RESET("ambari.cluster.ambari.database.reset"),
    AMBARI_CLUSTER_RESTARTING_AMBARI_SERVER("ambari.cluster.restarting.ambari.server"),
    AMBARI_CLUSTER_RESTARTING_AMBARI_AGENT("ambari.cluster.restarting.ambari.agent"),
    AMBARI_CLUSTER_AMBARI_AGENT_RESTARTED("ambari.cluster.ambari.agent.restarted"),
    AMBARI_CLUSTER_AMBARI_SERVER_RESTARTED("ambari.cluster.ambari.server.restarted"),
    AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP("ambari.cluster.removing.node.from.hostgroup"),
    AMBARI_CLUSTER_ADDING_NODE_TO_HOSTGROUP("ambari.cluster.adding.node.to.hostgroup"),
    AMBARI_CLUSTER_HOST_JOIN_FAILED("ambari.cluster.host.join.failed"),
    AMBARI_CLUSTER_INSTALL_FAILED("ambari.cluster.install.failed"),
    AMBARI_CLUSTER_UPSCALE_FAILED("ambari.cluster.upscale.failed"),
    AMBARI_CLUSTER_PREPARE_DEKERBERIZING_FAILED("ambari.cluster.prepare.dekerberizing.failed"),
    AMBARI_CLUSTER_PREPARE_DEKERBERIZING_ERROR("ambari.cluster.prepare.dekerberizing.error"),
    AMBARI_CLUSTER_DISABLE_KERBEROS_FAILED("ambari.cluster.disable.kerberos.failed"),
    AMBARI_CLUSTER_MR_SMOKE_FAILED("ambari.cluster.mr.smoke.failed"),
    AMBARI_CLUSTER_SERVICES_INIT_FAILED("ambari.cluster.services.init.failed"),
    AMBARI_REGENERATE_KERBEROS_KEYTABS_FAILED("ambari.regenerate.kerberos.keytabs.failed"),
    AMBARI_CLUSTER_SERVICES_STARTING("ambari.cluster.services.starting"),
    AMBARI_CLUSTER_SERVICES_START_FAILED("ambari.cluster.services.start.failed"),
    AMBARI_CLUSTER_SERVICES_STARTED("ambari.cluster.services.started"),
    AMBARI_CLUSTER_SERVICES_STOPPING("ambari.cluster.services.stopping"),
    AMBARI_CLUSTER_SERVICES_STOP_FAILED("ambari.cluster.services.stop.failed"),
    AMBARI_CLUSTER_SERVICES_STOPPED("ambari.cluster.services.stopped");

    private final String code;

    AmbariMessages(String msgCode) {
        code = msgCode;
    }

    public String code() {
        return code;
    }
}
