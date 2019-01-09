package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapTestV4Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @Valid
    @ApiModelProperty(ModelDescriptions.LdapConfigModelDescription.VALIDATION_REQUEST)
    private LdapTestConnectionV4Request validationRequest;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LdapTestConnectionV4Request getValidationRequest() {
        return validationRequest;
    }

    public void setValidationRequest(LdapTestConnectionV4Request validationRequest) {
        this.validationRequest = validationRequest;
    }
}
