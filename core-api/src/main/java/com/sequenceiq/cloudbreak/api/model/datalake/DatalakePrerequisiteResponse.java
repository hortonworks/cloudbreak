package com.sequenceiq.cloudbreak.api.model.datalake;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatalakePrerequisiteResponse extends ParametersQueryRequest {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.RESPONSE)
    private LdapConfigResponse ldapConfig;

    @ApiModelProperty(value = ModelDescriptions.RDSConfigModelDescription.RESPONSE)
    private Set<DatabaseV4Response> databases = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.KerberosConfigModelDescription.RESPONSE)
    private KerberosV4Response kerberos;

    public LdapConfigResponse getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfigResponse ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public Set<DatabaseV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<DatabaseV4Response> databases) {
        this.databases = databases;
    }

    public KerberosV4Response getKerberos() {
        return kerberos;
    }

    public void setKerberos(KerberosV4Response kerberos) {
        this.kerberos = kerberos;
    }
}
