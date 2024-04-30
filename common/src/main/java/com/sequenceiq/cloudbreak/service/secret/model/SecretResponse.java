package com.sequenceiq.cloudbreak.service.secret.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.doc.SecretResponseModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(Include.NON_NULL)
public class SecretResponse implements Serializable {

    @Schema(description = SecretResponseModelDescription.ENGINE_PATH)
    private String enginePath;

    @Schema(description = SecretResponseModelDescription.SECRET_PATH)
    private String secretPath;

    @Schema(description = SecretResponseModelDescription.SECRET_VERSION)
    private Integer secretVersion;

    public SecretResponse() {
    }

    public SecretResponse(String enginePath, String secretPath, Integer secretVersion) {
        this.enginePath = enginePath;
        this.secretPath = secretPath;
        this.secretVersion = secretVersion;
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

    public Integer getSecretVersion() {
        return secretVersion;
    }

    public void setSecretVersion(Integer secretVersion) {
        this.secretVersion = secretVersion;
    }

    @Override
    public String toString() {
        return "SecretResponse{" +
                "enginePath='" + enginePath + '\'' +
                ", secretPath='" + secretPath + '\'' +
                ", secretVersion=" + secretVersion +
                '}';
    }
}
