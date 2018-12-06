package com.sequenceiq.cloudbreak.api.model.datalake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatalakePrerequisiteResponse extends ParametersQueryRequest {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.LdapConfigModelDescription.RESPONSE)
    private LdapConfigResponse ldapConfig;

    @ApiModelProperty(value = ModelDescriptions.RDSConfigModelDescription.RESPONSE)
    private Set<RDSConfigResponse> rdsConfigs = new HashSet<>();

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.KerberosConfigModelDescription.RESPONSE)
    private KerberosResponse kerberosConfig;

    public LdapConfigResponse getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfigResponse ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public Set<RDSConfigResponse> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfigResponse> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public KerberosResponse getKerberosConfig() {
        return kerberosConfig;
    }

    public void setKerberosConfig(KerberosResponse kerberosConfig) {
        this.kerberosConfig = kerberosConfig;
    }
}
