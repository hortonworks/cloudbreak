package com.sequenceiq.cloudbreak.api.model.ldap;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LDAPTestRequest implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @Valid
    @ApiModelProperty(LdapConfigModelDescription.VALIDATION_REQUEST)
    private LdapValidationRequest validationRequest;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LdapValidationRequest getValidationRequest() {
        return validationRequest;
    }

    public void setValidationRequest(LdapValidationRequest validationRequest) {
        this.validationRequest = validationRequest;
    }
}
