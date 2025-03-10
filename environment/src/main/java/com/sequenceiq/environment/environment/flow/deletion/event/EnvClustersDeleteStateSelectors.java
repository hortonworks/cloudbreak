package com.sequenceiq.environment.environment.flow.deletion.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvClustersDeleteStateSelectors implements FlowEvent {

    START_DATAHUB_CLUSTERS_DELETE_EVENT,
    START_EXPERIENCE_DELETE_EVENT,
    START_DATALAKE_CLUSTERS_DELETE_EVENT,
    START_COMPUTE_CLUSTERS_DELETE_EVENT,
    FINISH_ENV_CLUSTERS_DELETE_EVENT,
    FAILED_ENV_CLUSTERS_DELETE_EVENT,
    HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT;

    @Override
    public String event() {
        return name();
    }

}
