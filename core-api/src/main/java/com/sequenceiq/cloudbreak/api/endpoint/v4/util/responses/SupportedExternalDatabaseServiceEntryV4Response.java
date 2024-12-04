package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;


import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SupportedDatabaseModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportedExternalDatabaseServiceEntryV4Response implements JsonEntity {

    @Schema(description = SupportedDatabaseModelDescription.NAME)
    private String name;

    @Schema(description = SupportedDatabaseModelDescription.SERVICE_DISPLAYNAME)
    private String displayName;

    @Schema(description = SupportedDatabaseModelDescription.DATABASES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<SupportedDatabaseEntryV4Response> databases = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<SupportedDatabaseEntryV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<SupportedDatabaseEntryV4Response> databases) {
        this.databases = databases;
    }
}
