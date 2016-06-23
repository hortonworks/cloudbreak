package com.sequenceiq.cloudbreak.core.flow2.stack.provision

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CheckImageAction

enum class StackCreationState : FlowState {
    INIT_STATE,
    STACK_CREATION_FAILED_STATE,
    SETUP_STATE,
    IMAGESETUP_STATE,
    IMAGE_CHECK_STATE(CheckImageAction::class.java),
    START_PROVISIONING_STATE,
    PROVISIONING_FINISHED_STATE,
    COLLECTMETADATA_STATE,
    TLS_SETUP_STATE,
    BOOTSTRAPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    STACK_CREATION_FINISHED_STATE,
    FINAL_STATE;

    private val action: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>

    private constructor() {
    }

    private constructor(action: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>) {
        this.action = action
    }

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>> {
        return action
    }
}
