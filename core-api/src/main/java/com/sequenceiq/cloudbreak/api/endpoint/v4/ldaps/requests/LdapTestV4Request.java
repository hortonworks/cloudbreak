package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapTestV4Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String existingLdapName;

    @Valid
    @ApiModelProperty(LdapConfigModelDescription.VALIDATION_REQUEST)
    private LdapMinimalV4Request ldap;

    public String getExistingLdapName() {
        return existingLdapName;
    }

    public void setExistingLdapName(String existingLdapName) {
        this.existingLdapName = existingLdapName;
    }

    public LdapMinimalV4Request getLdap() {
        return ldap;
    }

    public void setLdap(LdapMinimalV4Request ldap) {
        this.ldap = ldap;
    }
}
