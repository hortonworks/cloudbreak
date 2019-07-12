package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ExternalDatabaseServerStatusV4Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseServerStatusV4Response {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ExternalDatabaseServer.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CRN, required = true)
    private String resourceCrn;

    // @NotNull
    // @ApiModelProperty(value = ModelDescriptions.ExternalDatabaseServer.DATABASE_SERVER, required = true)
    // private DatabaseServerV4Response databaseServerConfig;

    @ApiModelProperty(value = ModelDescriptions.ExternalDatabaseServer.STATUS, required = true)
    private Status status;

    @ApiModelProperty(value = ModelDescriptions.ExternalDatabaseServer.STATUS_REASON, required = true)
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
