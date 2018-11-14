package com.sequenceiq.cloudbreak.api.model;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SecretResponse implements Serializable {

    @ApiModelProperty(ModelDescriptions.SecretResponseModelDescription.ENGINE_PATH)
    private String enginePath;

    @ApiModelProperty(ModelDescriptions.SecretResponseModelDescription.SECRET_PATH)
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
}
