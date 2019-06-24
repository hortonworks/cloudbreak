package com.sequenceiq.freeipa.api.v1.keytab.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.keytab.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ServiceKeytabV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceKeytabRequest {

    @ApiModelProperty(KeytabModelDescription.ID)
    private int id;

    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private @NotNull String environmentCrn;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_NAME, required = true)
    private @NotNull String serviceName;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_HOST, required = true)
    private @NotNull String serverHostName;

    @ApiModelProperty(ModelDescriptions.USER_CRN)
    private @NotNull String userCrn;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }
}
