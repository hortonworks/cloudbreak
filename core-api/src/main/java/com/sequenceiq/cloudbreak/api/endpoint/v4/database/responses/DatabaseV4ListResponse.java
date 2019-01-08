package com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DatabaseV4ListResponse {

    private Set<DatabaseV4Response> databases = new HashSet<>();

    public Set<DatabaseV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<DatabaseV4Response> databases) {
        this.databases = databases;
    }

    public static final DatabaseV4ListResponse databaseListResponse(Set<DatabaseV4Response> databases) {
        DatabaseV4ListResponse databaseV4ListResponse = new DatabaseV4ListResponse();
        databaseV4ListResponse.setDatabases(databases);
        return databaseV4ListResponse;
    }

}
