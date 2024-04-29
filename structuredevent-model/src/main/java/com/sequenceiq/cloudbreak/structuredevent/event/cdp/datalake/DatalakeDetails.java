package com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.structuredevent.event.DatabaseDetails;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatalakeDetails implements Serializable {

    private String cloudPlatform;

    private boolean razEnabled;

    private boolean multiAzEnabled;

    private DatabaseDetails databaseDetails;

    private String status;

    private String statusReason;

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public boolean isRazEnabled() {
        return razEnabled;
    }

    public void setRazEnabled(boolean razEnabled) {
        this.razEnabled = razEnabled;
    }

    public boolean isMultiAzEnabled() {
        return multiAzEnabled;
    }

    public void setMultiAzEnabled(boolean multiAzEnabled) {
        this.multiAzEnabled = multiAzEnabled;
    }

    public DatabaseDetails getDatabaseDetails() {
        return databaseDetails;
    }

    public void setDatabaseDetails(DatabaseDetails databaseDetails) {
        this.databaseDetails = databaseDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
