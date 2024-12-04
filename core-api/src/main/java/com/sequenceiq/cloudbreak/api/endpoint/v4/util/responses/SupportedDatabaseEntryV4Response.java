package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;


import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SupportedDatabaseModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportedDatabaseEntryV4Response implements JsonEntity {

    @Schema(description = SupportedDatabaseModelDescription.DATABASENAME)
    private String databaseName;

    @Schema(description = SupportedDatabaseModelDescription.DISPLAYNAME)
    private String displayName;

    @Schema(description = SupportedDatabaseModelDescription.JDBCPREFIX)
    private String jdbcPrefix;

    @Schema(description = SupportedDatabaseModelDescription.VERSIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> versions = new HashSet<>();

    public SupportedDatabaseEntryV4Response() {
    }

    public SupportedDatabaseEntryV4Response(String databaseName, String displayName, String jdbcPrefix, Set<String> versions) {
        this.databaseName = databaseName;
        this.displayName = displayName;
        this.jdbcPrefix = jdbcPrefix;
        this.versions = versions;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getJdbcPrefix() {
        return jdbcPrefix;
    }

    public void setJdbcPrefix(String jdbcPrefix) {
        this.jdbcPrefix = jdbcPrefix;
    }

    public Set<String> getVersions() {
        return versions;
    }

    public void setVersions(Set<String> versions) {
        this.versions = versions;
    }
}
