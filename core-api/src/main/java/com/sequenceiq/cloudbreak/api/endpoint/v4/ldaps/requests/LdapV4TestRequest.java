package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapV4TestRequest implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @Valid
    @ApiModelProperty(ModelDescriptions.LdapConfigModelDescription.VALIDATION_REQUEST)
    private LdapV4ValidationRequest validationRequest;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LdapV4ValidationRequest getValidationRequest() {
        return validationRequest;
    }

    public void setValidationRequest(LdapV4ValidationRequest validationRequest) {
        this.validationRequest = validationRequest;
    }
}
