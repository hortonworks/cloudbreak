package com.sequenceiq.redbeams.flow.redbeams.sslmigration;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerSuccessResult;

public enum RedbeamsSslMigrationEventSelectors implements FlowEvent {

    REDBEAMS_SSL_MIGRATION_EVENT(),
    REDBEAMS_SSL_MIGRATION_FINISHED_EVENT(EventSelectorUtil.selector(RedbeamsSslMigrationHandlerSuccessResult.class)),
    REDBEAMS_SSL_MIGRATION_FAILED_EVENT(EventSelectorUtil.selector(RedbeamsSslMigrationFailed.class)),
    REDBEAMS_SSL_MIGRATION_FINALIZED_EVENT(),
    REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT();

    private final String event;

    RedbeamsSslMigrationEventSelectors(String event) {
        this.event = event;
    }

    RedbeamsSslMigrationEventSelectors() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
