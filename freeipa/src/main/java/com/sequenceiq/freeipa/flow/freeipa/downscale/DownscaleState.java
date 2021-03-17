package com.sequenceiq.freeipa.flow.freeipa.downscale;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum DownscaleState implements FlowState {
    INIT_STATE,
    STARTING_DOWNSCALE_STATE,
    DOWNSCALE_CLUSTERPROXY_REGISTRATION_STATE,
    DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_STATE,
    DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_STATE,
    DOWNSCALE_STOP_TELEMETRY_STATE,
    DOWNSCALE_COLLECT_RESOURCES_STATE,
    DOWNSCALE_REMOVE_INSTANCES_STATE,
    DOWNSCALE_REMOVE_SERVERS_STATE,
    DOWNSCALE_REVOKE_CERTS_STATE,
    DOWNSCALE_REMOVE_DNS_ENTRIES_STATE,
    DOWNSCALE_UPDATE_DNS_SOA_RECORDS_STATE,
    DOWNSCALE_UPDATE_METADATA_STATE,
    DOWNSCALE_REMOVE_HOSTS_FROM_ORCHESTRATION_STATE,
    DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE,
    DOWNSCALE_FINISHED_STATE,
    DOWNSCALE_FAIL_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
