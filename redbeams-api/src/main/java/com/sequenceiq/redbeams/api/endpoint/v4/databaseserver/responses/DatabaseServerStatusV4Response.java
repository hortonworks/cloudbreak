package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.DATABASE_SERVER_STATUS_RESPONSE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseServerStatusV4Response {
    @NotNull
    @Schema(description = DatabaseServer.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @Schema(description = DatabaseServer.NAME, required = true)
    private String name;

    @NotNull
    @Schema(description = DatabaseServer.CRN, required = true)
    private String resourceCrn;

    @Schema(description = DatabaseServer.STATUS, required = true)
    private Status status;

    @Schema(description = DatabaseServer.STATUS_REASON, required = true)
    private String statusReason;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
