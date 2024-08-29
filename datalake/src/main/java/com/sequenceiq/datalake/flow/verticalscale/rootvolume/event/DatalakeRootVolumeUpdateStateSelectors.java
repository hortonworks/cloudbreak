package com.sequenceiq.datalake.flow.verticalscale.rootvolume.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeRootVolumeUpdateStateSelectors implements FlowEvent {
    DATALAKE_ROOT_VOLUME_UPDATE_EVENT,
    DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT,
    DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT,
    DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT,
    HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT,
    FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
