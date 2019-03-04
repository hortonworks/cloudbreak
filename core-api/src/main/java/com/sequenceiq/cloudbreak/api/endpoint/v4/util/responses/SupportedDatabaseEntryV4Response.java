package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;


import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SupportedDatabaseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SupportedDatabaseEntryV4Response implements JsonEntity {

    @ApiModelProperty(SupportedDatabaseModelDescription.DATABASENAME)
    private String databaseName;

    @ApiModelProperty(SupportedDatabaseModelDescription.DISPLAYNAME)
    private String displayName;

    @ApiModelProperty(SupportedDatabaseModelDescription.JDBCPREFIX)
    private String jdbcPrefix;

    @ApiModelProperty(SupportedDatabaseModelDescription.VERSIONS)
    private  Set<String> versions;

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
