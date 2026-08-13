package com.sequenceiq.datalake.flow.encryptionprofile;

import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxEnableEncryptionProfileEvent implements FlowEvent {

    SDX_ENABLE_ENCRYPTION_PROFILE_EVENT,
    SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT,
    SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_EVENT(SdxEnableEncryptionProfileFailedEvent.class),
    SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT,
    SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;

    private final String event;

    SdxEnableEncryptionProfileEvent() {
        event = name();
    }

    SdxEnableEncryptionProfileEvent(Class<?> eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
