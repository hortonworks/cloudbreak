package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ServiceKeytabV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceKeytabRequest {

    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @NotNull
    private String environmentCrn;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_NAME, required = true)
    @NotNull
    private String serviceName;

    @ApiModelProperty(value = KeytabModelDescription.SERVICE_HOST, required = true)
    @NotNull
    private String serverHostName;

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
}
