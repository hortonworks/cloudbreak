package com.sequenceiq.datalake.flow.upgrade;

import com.sequenceiq.flow.core.FlowEvent;

public enum SdxOsUpgradeEvent implements FlowEvent {

    SDX_UPGRADE_EVENT("SDX_UPGRADE_EVENT"),
    SDX_UPGRADE_FAILED_TO_START_EVENT("SDX_UPGRADE_FAILED_TO_START_EVENT"),
    SDX_IMAGE_CHANGED_EVENT("SdxImageChangedEvent"),
    SDX_UPGRADE_IN_PROGRESS_EVENT("SDX_UPGRADE_IN_PROGRESS_EVENT"),
    SDX_UPGRADE_SUCCESS_EVENT("SdxUpgradeSuccessEvent"),
    SDX_UPGRADE_FAILED_EVENT("SdxUpgradeFailedEvent"),
    SDX_UPGRADE_FAILED_HANDLED_EVENT("SDX_UPGRADE_FAILED_HANDLED_EVENT"),
    SDX_UPGRADE_FINALIZED_EVENT("SDX_UPGRADE_FINALIZED_EVENT");

    private final String event;

    SdxOsUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
