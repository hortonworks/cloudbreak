package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update;

import com.sequenceiq.flow.core.FlowState;

public enum PillarConfigUpdateState implements FlowState {
    INIT_STATE,
    PILLAR_CONFIG_UPDATE_START_STATE,
    PILLAR_CONFIG_UPDATE_FINISHED_STATE,
    FINAL_STATE,
    PILLAR_CONFIG_UPDATE_FAILED_STATE;
}