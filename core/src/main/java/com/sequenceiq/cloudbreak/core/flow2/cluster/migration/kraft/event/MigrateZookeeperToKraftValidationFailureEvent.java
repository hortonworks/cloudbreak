package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class MigrateZookeeperToKraftValidationFailureEvent extends StackFailureEvent {

    private final Exception exception;

    @JsonCreator
    public MigrateZookeeperToKraftValidationFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event(), resourceId, exception);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "MigrateZookeeperToKraftValidationFailureEvent{" +
                "selector='" + selector() + '\'' +
                ", exception='" + exception + '\'' +
                '}' + super.toString();
    }
}
