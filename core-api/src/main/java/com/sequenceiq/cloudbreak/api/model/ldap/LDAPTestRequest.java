package com.sequenceiq.cloudbreak.api.model.ldap;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LDAPTestRequest implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @Valid
    @ApiModelProperty(ModelDescriptions.LdapConfigModelDescription.VALIDATION_REQUEST)
    private LdapValidationRequest validationRequest;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LdapValidationRequest getValidationRequest() {
        return validationRequest;
    }

    public void setValidationRequest(LdapValidationRequest validationRequest) {
        this.validationRequest = validationRequest;
    }
}
