package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackDownscaleState implements FlowState {
    INIT_STATE,
    DOWNSCALE_FAILED_STATE,
    DOWNSCALE_STATE,
    DOWNSCALE_FINISHED_STATE,
    FINAL_STATE
}
