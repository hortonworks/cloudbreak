package com.sequenceiq.environment.api.credential.model.parameters.yarn;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class YarnCredentialV1Parameters implements Serializable {

    @ApiModelProperty(required = true)
    private String ambariUser;

    @ApiModelProperty(required = true)
    private String endpoint;

    public String getAmbariUser() {
        return ambariUser;
    }

    public void setAmbariUser(String ambariUser) {
        this.ambariUser = ambariUser;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
