package com.sequenceiq.redbeams.flow.redbeams.sslmigration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class RedbeamsSslMigrationEvent extends RedbeamsEvent implements IdempotentEvent<RedbeamsEvent> {

    @JsonCreator
    public RedbeamsSslMigrationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId) {
        super(selector, resourceId, new Promise<>(), true);
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(RedbeamsSslMigrationEvent.class, other);
    }
}