package com.sequenceiq.freeipa.api.model.credential;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class Secret implements Serializable {

    private String enginePath;

    private String secretPath;

    public Secret() {
    }

    public Secret(String enginePath, String secretPath) {
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
