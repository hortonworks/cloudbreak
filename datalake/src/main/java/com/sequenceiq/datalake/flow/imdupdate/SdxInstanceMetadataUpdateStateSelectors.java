package com.sequenceiq.datalake.flow.imdupdate;

import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateSuccessEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateWaitSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxInstanceMetadataUpdateStateSelectors implements FlowEvent {

    SDX_IMD_UPDATE_EVENT,
    SDX_IMD_UPDATE_SUCCESS_EVENT(SdxInstanceMetadataUpdateSuccessEvent.class),
    SDX_IMD_UPDATE_WAIT_SUCCESS_EVENT(SdxInstanceMetadataUpdateWaitSuccessEvent.class),
    SDX_IMD_UPDATE_FAILED_EVENT(SdxInstanceMetadataUpdateFailedEvent.class),
    SDX_IMD_UPDATE_FAILED_HANDLED_EVENT,
    SDX_IMD_UPDATE_FINALIZED_EVENT;

    private final String event;

    SdxInstanceMetadataUpdateStateSelectors() {
        event = name();
    }

    SdxInstanceMetadataUpdateStateSelectors(Class<?> eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }

}
