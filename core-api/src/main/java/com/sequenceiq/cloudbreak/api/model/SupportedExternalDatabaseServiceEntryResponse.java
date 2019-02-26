package com.sequenceiq.cloudbreak.api.model;


import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SupportedDatabaseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SupportedExternalDatabaseServiceEntryResponse")
public class SupportedExternalDatabaseServiceEntryResponse implements JsonEntity {

    @ApiModelProperty(SupportedDatabaseModelDescription.NAME)
    private String name;

    @ApiModelProperty(SupportedDatabaseModelDescription.SERVICE_DISPLAYNAME)
    private String displayName;

    @ApiModelProperty(SupportedDatabaseModelDescription.DATABASES)
    private Set<SupportedDatabaseEntryResponse> databases = new HashSet<>();

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

    public Set<SupportedDatabaseEntryResponse> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<SupportedDatabaseEntryResponse> databases) {
        this.databases = databases;
    }
}
