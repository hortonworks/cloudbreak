package com.sequenceiq.environment.environment.flow.deletion.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvDeleteHandlerSelectors implements FlowEvent {

    DELETE_NETWORK_EVENT,
    DELETE_RDBMS_EVENT,
    DELETE_CLUSTER_DEFINITION_EVENT,
    DELETE_FREEIPA_EVENT,
    UNSCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT,
    DELETE_EXPERIENCE_EVENT,
    DELETE_IDBROKER_MAPPINGS_EVENT,
    DELETE_S3GUARD_TABLE_EVENT,
    DELETE_UMS_RESOURCE_EVENT,
    DELETE_DATAHUB_CLUSTERS_EVENT,
    DELETE_DATALAKE_CLUSTERS_EVENT,
    DELETE_PUBLICKEY_EVENT,
    DELETE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;

    @Override
    public String event() {
        return name();
    }

}
