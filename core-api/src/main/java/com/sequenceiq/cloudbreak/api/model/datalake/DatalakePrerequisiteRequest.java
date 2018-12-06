package com.sequenceiq.cloudbreak.api.model.datalake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatalakePrerequisiteRequest extends ParametersQueryRequest {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.REQUEST, required = true)
    private LdapConfigRequest ldapConfig;

    @ApiModelProperty(value = ModelDescriptions.RDSConfigModelDescription.REQUEST, required = true)
    private Set<RDSConfigRequest> rdsConfigs = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.KerberosConfigModelDescription.REQUEST, required = true)
    private KerberosRequest kerberosConfig;

    public LdapConfigRequest getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfigRequest ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public Set<RDSConfigRequest> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfigRequest> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public KerberosRequest getKerberosConfig() {
        return kerberosConfig;
    }

    public void setKerberosConfig(KerberosRequest kerberosConfig) {
        this.kerberosConfig = kerberosConfig;
    }
}
