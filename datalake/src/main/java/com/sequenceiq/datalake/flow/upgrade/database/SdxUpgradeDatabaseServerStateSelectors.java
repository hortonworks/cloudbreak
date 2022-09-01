package com.sequenceiq.datalake.flow.upgrade.database;

import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.database.event.SdxUpgradeDatabaseServerSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxUpgradeDatabaseServerStateSelectors implements FlowEvent {

    SDX_UPGRADE_DATABASE_SERVER_UPGRADE_EVENT,
    SDX_UPGRADE_DATABASE_SERVER_SUCCESS_EVENT(SdxUpgradeDatabaseServerSuccessEvent.class),
    SDX_UPGRADE_DATABASE_SERVER_FAILED_EVENT(SdxUpgradeDatabaseServerFailedEvent.class),
    SDX_UPGRADE_DATABASE_SERVER_FAILED_HANDLED_EVENT,
    SDX_UPGRADE_DATABASE_SERVER_FINALIZED_EVENT;

    private final String event;

    SdxUpgradeDatabaseServerStateSelectors() {
        event = name();
    }

    SdxUpgradeDatabaseServerStateSelectors(Class<?> eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }

}
