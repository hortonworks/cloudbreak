package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;


import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SupportedDatabaseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SupportedExternalDatabaseServiceEntryV4Response implements JsonEntity {

    @ApiModelProperty(SupportedDatabaseModelDescription.NAME)
    private String name;

    @ApiModelProperty(SupportedDatabaseModelDescription.SERVICE_DISPLAYNAME)
    private String displayName;

    @ApiModelProperty(SupportedDatabaseModelDescription.DATABASES)
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
