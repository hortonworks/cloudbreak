package com.sequenceiq.cloudbreak.api.model;


import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SupportedDatabaseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SupportedDatabaseEntryResponse")
public class SupportedDatabaseEntryResponse implements JsonEntity {

    @ApiModelProperty(value = SupportedDatabaseModelDescription.DATABASENAME)
    private String databaseName;

    @ApiModelProperty(value = SupportedDatabaseModelDescription.DISPLAYNAME)
    private String displayName;

    @ApiModelProperty(value = SupportedDatabaseModelDescription.JDBCPREFIX)
    private String jdbcPrefix;

    public SupportedDatabaseEntryResponse() {
    }

    public SupportedDatabaseEntryResponse(String databaseName, String displayName, String jdbcPrefix) {
        this.databaseName = databaseName;
        this.displayName = displayName;
        this.jdbcPrefix = jdbcPrefix;
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
}
