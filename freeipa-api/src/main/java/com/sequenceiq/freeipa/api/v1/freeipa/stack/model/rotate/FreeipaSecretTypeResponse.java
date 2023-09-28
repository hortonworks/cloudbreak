package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeipaSecretTypeResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeipaSecretTypeResponse {

    @NotNull
    private String secretType;

    private String description;

    public FreeipaSecretTypeResponse() {
    }

    public FreeipaSecretTypeResponse(String secretType, String description) {
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
        return "FreeipaSecretTypeResponse{" +
                "secretType='" + secretType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
