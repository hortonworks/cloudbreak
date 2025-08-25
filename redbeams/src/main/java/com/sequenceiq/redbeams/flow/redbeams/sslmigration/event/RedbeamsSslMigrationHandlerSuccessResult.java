package com.sequenceiq.redbeams.flow.redbeams.sslmigration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class RedbeamsSslMigrationHandlerSuccessResult extends RedbeamsEvent {

    @JsonCreator
    public RedbeamsSslMigrationHandlerSuccessResult(
            @JsonProperty("resourceId") Long resourceId) {
        super(EventSelectorUtil.selector(RedbeamsSslMigrationHandlerSuccessResult.class), resourceId, true);
    }
}
