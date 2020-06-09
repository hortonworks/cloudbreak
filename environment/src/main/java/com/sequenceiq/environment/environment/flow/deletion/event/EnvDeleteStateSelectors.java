package com.sequenceiq.environment.environment.flow.deletion.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvDeleteStateSelectors implements FlowEvent {

    START_NETWORK_DELETE_EVENT,
    START_RDBMS_DELETE_EVENT,
    START_FREEIPA_DELETE_EVENT,
    START_IDBROKER_MAPPINGS_DELETE_EVENT,
    START_S3GUARD_TABLE_DELETE_EVENT,
    START_CLUSTER_DEFINITION_CLEANUP_EVENT,
    START_UMS_RESOURCE_DELETE_EVENT,
    START_PUBLICKEY_DELETE_EVENT,
    START_PREREQUISITES_DELETE_EVENT,
    FINISH_ENV_DELETE_EVENT,
    FINALIZE_ENV_DELETE_EVENT,
    FAILED_ENV_DELETE_EVENT,
    HANDLED_FAILED_ENV_DELETE_EVENT;

    @Override
    public String event() {
        return name();
    }

}
