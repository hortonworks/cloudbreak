package com.sequenceiq.environment.api.v1.credential.model.parameters.aws;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("KeyBasedV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KeyBasedParameters implements Serializable {

    @NotNull
    @ApiModelProperty(required = true, example = "ASIBJ34QYCJ1IBLK24KA")
    private String accessKey;

    @NotNull
    @ApiModelProperty(required = true, example = "Ratk5cM9edxGuN6jdGb/8Jf621ZuTVGkoO14GPwN")
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
