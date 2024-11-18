package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;

public class ExternalDatabaseUserFlowStartEvent extends ExternalDatabaseSelectableEvent {

    private final ExternalDatabaseUserOperation operation;

    private final DatabaseType databaseType;

    private final String databaseUser;

    @JsonCreator
    public ExternalDatabaseUserFlowStartEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("operation") ExternalDatabaseUserOperation operation,
            @JsonProperty("databaseType") DatabaseType databaseType,
            @JsonProperty("databaseUser") String databaseUser) {
        super(resourceId, selector, resourceName, resourceCrn);
        this.operation = operation;
        this.databaseUser = databaseUser;
        this.databaseType = databaseType;
    }

    public ExternalDatabaseUserOperation getOperation() {
        return operation;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }
}
