package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DatabaseV4Responses {

    private Set<DatabaseV4Response> databases = new HashSet<>();

    public Set<DatabaseV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<DatabaseV4Response> databases) {
        this.databases = databases;
    }

    public static final DatabaseV4Responses databaseListResponse(Set<DatabaseV4Response> databases) {
        DatabaseV4Responses databaseV4Responses = new DatabaseV4Responses();
        databaseV4Responses.setDatabases(databases);
        return databaseV4Responses;
    }

}
