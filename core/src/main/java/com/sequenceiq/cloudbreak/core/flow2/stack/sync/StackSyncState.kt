package com.sequenceiq.cloudbreak.core.flow2.stack.sync

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class StackSyncState : FlowState {
    INIT_STATE,
    SYNC_FAILED_STATE,
    SYNC_STATE,
    SYNC_FINISHED_STATE,
    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
