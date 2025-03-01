package com.sequenceiq.environment.api.v1.credential.model.parameters.aws;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "KeyBasedV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KeyBasedParameters implements Serializable {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "ASIBJ34QYCJ1IBLK24KA")
    private String accessKey;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Ratk5cM9edxGuN6jdGb/8Jf621ZuTVGkoO14GPwN")
    private String secretKey;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return "KeyBasedParameters{" +
                "accessKey='" + accessKey + '\'' +
                '}';
    }
}
