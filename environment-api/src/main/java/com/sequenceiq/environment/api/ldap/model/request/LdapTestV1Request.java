package com.sequenceiq.environment.api.ldap.model.request;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.ldap.doc.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapTestV1Request implements Serializable {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String existingLdapName;

    @Valid
    @ApiModelProperty(LdapConfigModelDescription.VALIDATION_REQUEST)
    private LdapMinimalV1Request ldap;

    public String getExistingLdapName() {
        return existingLdapName;
    }

    public void setExistingLdapName(String existingLdapName) {
        this.existingLdapName = existingLdapName;
    }

    public LdapMinimalV1Request getLdap() {
        return ldap;
    }

    public void setLdap(LdapMinimalV1Request ldap) {
        this.ldap = ldap;
    }
}
