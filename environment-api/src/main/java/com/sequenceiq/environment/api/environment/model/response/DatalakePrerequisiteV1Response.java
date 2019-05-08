package com.sequenceiq.environment.api.environment.model.response;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.ldap.model.response.LdapV1Response;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatalakePrerequisiteV1Response implements Serializable {

    @NotNull
    @ApiModelProperty(EnvironmentModelDescription.LDAP_RESPONSE)
    private LdapV1Response ldap;

    @ApiModelProperty(EnvironmentModelDescription.RDS_RESPONSE)
    private Set<String> databases = new HashSet<>();

    @NotNull
    @ApiModelProperty(EnvironmentModelDescription.KERBEROS_RESPONSE)
    private String kerberos;

    public LdapV1Response getLdap() {
        return ldap;
    }

    public void setLdap(LdapV1Response ldap) {
        this.ldap = ldap;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<String> databases) {
        this.databases = databases;
    }

    public String getKerberos() {
        return kerberos;
    }

    public void setKerberos(String kerberos) {
        this.kerberos = kerberos;
    }
}
