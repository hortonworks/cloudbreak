package com.sequenceiq.environment.api.environment.model.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.environment.api.environment.doc.EnvironmentLdapDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatalakePrerequisiteV4Request {

    @NotNull
    @ApiModelProperty(value = EnvironmentLdapDescription.LDAP_REQUEST, required = true)
    private LdapV4Request ldap;

    public LdapV4Request getLdap() {
        return ldap;
    }

    public void setLdap(LdapV4Request ldap) {
        this.ldap = ldap;
    }
}