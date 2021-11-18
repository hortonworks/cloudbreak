package com.sequenceiq.cloudbreak.service.secret.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.doc.SecretResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class SecretResponse implements Serializable {

    @ApiModelProperty(SecretResponseModelDescription.ENGINE_PATH)
    private String enginePath;

    @ApiModelProperty(SecretResponseModelDescription.SECRET_PATH)
    private String secretPath;

    public SecretResponse() {
    }

    public SecretResponse(String enginePath, String secretPath) {
        this.enginePath = enginePath;
        this.secretPath = secretPath;
    }

    public String getEnginePath() {
        return enginePath;
    }

    public void setEnginePath(String enginePath) {
        this.enginePath = enginePath;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public void setSecretPath(String secretPath) {
        this.secretPath = secretPath;
    }

    @Override
    public String toString() {
        return "SecretResponse{" +
                "enginePath='" + enginePath + '\'' +
                ", secretPath='" + secretPath + '\'' +
                '}';
    }
}
