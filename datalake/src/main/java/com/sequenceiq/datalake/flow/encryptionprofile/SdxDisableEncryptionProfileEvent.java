package com.sequenceiq.datalake.flow.encryptionprofile;

import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxDisableEncryptionProfileEvent implements FlowEvent {

    SDX_DISABLE_ENCRYPTION_PROFILE_EVENT,
    SDX_DISABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT,
    SDX_DISABLE_ENCRYPTION_PROFILE_FAILED_EVENT(SdxDisableEncryptionProfileFailedEvent.class),
    SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT,
    SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;

    private final String event;

    SdxDisableEncryptionProfileEvent() {
        event = name();
    }

    SdxDisableEncryptionProfileEvent(Class<?> eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
