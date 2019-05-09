package com.sequenceiq.environment.api.environment.model.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.LdapV4Request;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatalakePrerequisiteV1Request {

    @NotNull
    @ApiModelProperty(value = EnvironmentModelDescription.LDAP_REQUEST, required = true)
    private LdapV4Request ldap;

    public LdapV4Request getLdap() {
        return ldap;
    }

    public void setLdap(LdapV4Request ldap) {
        this.ldap = ldap;
    }
}