package com.sequenceiq.environment.api.environment.model.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.ldap.model.request.LdapV1Request;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatalakePrerequisiteV1Request {

    @NotNull
    @ApiModelProperty(value = EnvironmentModelDescription.LDAP_REQUEST, required = true)
    private LdapV1Request ldap;

    public LdapV1Request getLdap() {
        return ldap;
    }

    public void setLdap(LdapV1Request ldap) {
        this.ldap = ldap;
    }
}