package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.KerberosConfigModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.LdapConfigModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfigModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatalakePrerequisiteV4Request {

    @NotNull
    @ApiModelProperty(value = LdapConfigModelDescription.REQUEST, required = true)
    private LdapV4Request ldap;

    @ApiModelProperty(value = RDSConfigModelDescription.REQUEST, required = true)
    private Set<DatabaseV4Request> databases = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = KerberosConfigModelDescription.REQUEST, required = true)
    private KerberosV4Request kerberos;

    public LdapV4Request getLdap() {
        return ldap;
    }

    public void setLdap(LdapV4Request ldap) {
        this.ldap = ldap;
    }

    public Set<DatabaseV4Request> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<DatabaseV4Request> databases) {
        this.databases = databases;
    }

    public KerberosV4Request getKerberos() {
        return kerberos;
    }

    public void setKerberos(KerberosV4Request kerberos) {
        this.kerberos = kerberos;
    }
}