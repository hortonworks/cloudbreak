package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class MigrateZookeeperToKraftFailureEvent extends StackFailureEvent {

    private final Exception exception;

    @JsonCreator
    public MigrateZookeeperToKraftFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event(), resourceId, exception);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftFailureEvent{" +
                "selector='" + selector() + '\'' +
                ", exception='" + exception + '\'' +
                '}' + super.toString();
    }
}
