package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel("AccountPreferencesResponse")
public class AccountPreferencesResponse extends AccountPreferencesBase {

    private Set<SupportedExternalDatabaseServiceEntryResponse> supportedExternalDatabases = new HashSet<>();

    public Set<SupportedExternalDatabaseServiceEntryResponse> getSupportedExternalDatabases() {
        return supportedExternalDatabases;
    }

    public void setSupportedExternalDatabases(Set<SupportedExternalDatabaseServiceEntryResponse> supportedExternalDatabases) {
        this.supportedExternalDatabases = supportedExternalDatabases;
    }
}
