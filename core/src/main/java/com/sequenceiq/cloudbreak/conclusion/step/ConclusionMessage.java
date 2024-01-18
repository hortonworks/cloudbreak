package com.sequenceiq.cloudbreak.conclusion.step;

class ConclusionMessage {

    public static final String GATEWAY_NETWORK_STATUS_FAILED = "conclusion.gateway.network.status.failed";

    public static final String GATEWAY_SERVICES_STATUS_FAILED = "conclusion.gateway.services.status.failed";

    public static final String SERVICES_CHECK_FAILED = "conclusion.services.check.failed";

    public static final String SERVICES_CHECK_FAILED_DETAILS = "conclusion.services.check.failed.details";

    public static final String SALT_COLLECT_UNREACHABLE_FOUND = "conclusion.salt.collect.unreachable.found";

    public static final String SALT_COLLECT_UNREACHABLE_FOUND_DETAILS = "conclusion.salt.collect.unreachable.found.details";

    public static final String SALT_COLLECT_UNREACHABLE_FAILED = "conclusion.salt.collect.unreachable.failed";

    public static final String SALT_COLLECT_UNREACHABLE_FAILED_DETAILS = "conclusion.salt.collect.unreachable.failed.details";

    public static final String SALT_MASTER_SERVICES_UNHEALTHY = "conclusion.salt.master.services.unhealthy";

    public static final String SALT_MASTER_SERVICES_UNHEALTHY_DETAILS = "conclusion.salt.master.services.unhealthy.details";

    public static final String SALT_MINIONS_UNREACHABLE = "conclusion.salt.minions.unreachable";

    public static final String SALT_MINIONS_UNREACHABLE_DETAILS = "conclusion.salt.minions.unreachable.details";

    public static final String NETWORK_NGINX_UNREACHABLE = "conclusion.network.nginx.unreachable";

    public static final String NETWORK_CCM_NOT_ACCESSIBLE = "conclusion.network.ccm.not.accessible";

    public static final String NETWORK_CCM_NOT_ACCESSIBLE_DETAILS = "conclusion.network.ccm.not.accessible.details";

    public static final String NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE = "conclusion.network.cloudera.com.not.accessible";

    public static final String NETWORK_CLOUDERA_COM_NOT_ACCESSIBLE_DETAILS = "conclusion.network.cloudera.com.not.accessible.details";

    public static final String NETWORK_NEIGHBOUR_NOT_ACCESSIBLE = "conclusion.network.neighbour.not.accessible";

    public static final String NETWORK_NEIGHBOUR_NOT_ACCESSIBLE_DETAILS = "conclusion.network.neighbour.not.accessible.details";

    public static final String CM_SERVER_UNREACHABLE = "conclusion.cm.server.unreachable";

    public static final String CM_UNHEALTHY_VMS_FOUND = "conclusion.cm.unhealthy.vms.found";

    public static final String CM_UNHEALTHY_VMS_FOUND_DETAILS = "conclusion.cm.unhealthy.vms.found.details";

    public static final String PROVIDER_NOT_RUNNING_VMS_FOUND = "conclusion.provider.not.running.vms.found";

    public static final String PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS = "conclusion.provider.not.running.vms.found.details";

    private ConclusionMessage() {
    }

}
