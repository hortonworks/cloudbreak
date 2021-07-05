package com.sequenceiq.environment.api.v1.credential.model.parameters.gcp;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("JsonV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class JsonParameters implements Serializable {

    @NotNull
    @ApiModelProperty(required = true)
    private String credentialJson;

    public String getCredentialJson() {
        return credentialJson;
    }

    public void setCredentialJson(String credentialJson) {
        this.credentialJson = credentialJson;
    }

    @Override
    public String toString() {
        return "JsonParameters{}";
    }
}
