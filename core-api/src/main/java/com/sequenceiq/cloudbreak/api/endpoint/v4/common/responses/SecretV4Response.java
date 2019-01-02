package com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SecretV4Response implements Serializable {

    @ApiModelProperty(ModelDescriptions.SecretResponseModelDescription.ENGINE_PATH)
    private String enginePath;

    @ApiModelProperty(ModelDescriptions.SecretResponseModelDescription.SECRET_PATH)
    private String secretPath;

    public SecretV4Response() {
    }

    public SecretV4Response(String enginePath, String secretPath) {
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
