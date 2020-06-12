package com.sequenceiq.freeipa.api.v1.ldap.model.test;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("TestLdapConfigV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestLdapConfigRequest {
    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_CRN)
    @ResourceObjectField(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, variableType = AuthorizationVariableType.CRN)
    private String environmentCrn;

    @Valid
    @ApiModelProperty(LdapConfigModelDescription.VALIDATION_REQUEST)
    private MinimalLdapConfigRequest ldap;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public MinimalLdapConfigRequest getLdap() {
        return ldap;
    }

    public void setLdap(MinimalLdapConfigRequest ldap) {
        this.ldap = ldap;
    }
}
