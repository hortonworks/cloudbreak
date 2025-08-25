package com.sequenceiq.redbeams.flow.redbeams.sslmigration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

public class RedbeamsSslMigrationFailed extends RedbeamsFailureEvent {

    @JsonCreator
    public RedbeamsSslMigrationFailed(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(EventSelectorUtil.selector(RedbeamsSslMigrationFailed.class), resourceId, exception, true);
    }

    @Override
    public String toString() {
        return "SslCertRotateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
