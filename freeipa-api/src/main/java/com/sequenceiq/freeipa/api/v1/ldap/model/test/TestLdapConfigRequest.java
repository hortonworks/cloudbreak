package com.sequenceiq.freeipa.api.v1.ldap.model.test;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("TestLdapConfigV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestLdapConfigRequest {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String existingLdapName;

    @Valid
    @ApiModelProperty(LdapConfigModelDescription.VALIDATION_REQUEST)
    private MinimalLdapConfigRequest ldap;

    public String getExistingLdapName() {
        return existingLdapName;
    }

    public void setExistingLdapName(String existingLdapName) {
        this.existingLdapName = existingLdapName;
    }

    public MinimalLdapConfigRequest getLdap() {
        return ldap;
    }

    public void setLdap(MinimalLdapConfigRequest ldap) {
        this.ldap = ldap;
    }
}
