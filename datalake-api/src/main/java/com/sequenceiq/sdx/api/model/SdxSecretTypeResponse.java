package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSecretTypeResponse {

    @NotNull
    @ApiModelProperty("Secret type")
    private String secretType;

    @ApiModelProperty("Description")
    private String description;

    public SdxSecretTypeResponse() {
    }

    public SdxSecretTypeResponse(String secretType, String description) {
        this.secretType = secretType;
        this.description = description;
    }

    public String getSecretType() {
        return secretType;
    }

    public void setSecretType(String secretType) {
        this.secretType = secretType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "SdxSecretTypeResponse{" +
                "secretType='" + secretType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
