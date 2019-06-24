package com.sequenceiq.freeipa.api.v1.keytab.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.keytab.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ServiceKeytabV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserKeytabRequest {

    @ApiModelProperty(KeytabModelDescription.ID)
    private int id;

    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private @NotNull String environmentCrn;

    @ApiModelProperty(value = KeytabModelDescription.USER_NAME, required = true)
    private @NotNull String userName;

    @ApiModelProperty(value = KeytabModelDescription.USER_HOST, required = true)
    private @NotNull String hostName;

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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String serviceName) {
        this.userName = serviceName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }
}
