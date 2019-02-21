package com.sequenceiq.cloudbreak.cm;

public enum ClouderaManagerMessages {

    CM_CLUSTER_SERVICES_STOPPING("cm.cluster.services.stopping"),
    CM_CLUSTER_SERVICES_STOPPED("cm.cluster.services.stopped"),
    CM_CLUSTER_SERVICES_STARTED("cm.cluster.services.started"),
    CM_CLUSTER_SERVICES_STARTING("cm.cluster.services.starting");

    private final String code;

    ClouderaManagerMessages(String msgCode) {
        code = msgCode;
    }

    public String code() {
        return code;
    }
}
