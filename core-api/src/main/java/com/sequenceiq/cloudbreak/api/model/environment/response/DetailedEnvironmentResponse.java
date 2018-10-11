package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {

    @ApiModelProperty(EnvironmentResponseModelDescription.CREDENTIAL)
    private CredentialResponse credential;

    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIGS)
    private Set<ProxyConfigResponse> proxyConfigs;

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS)
    private Set<LdapConfigResponse> ldapConfigs;

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIGS)
    private Set<RDSConfigResponse> rdsConfigs;

    public CredentialResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialResponse credential) {
        this.credential = credential;
    }

    public Set<ProxyConfigResponse> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<ProxyConfigResponse> proxyConfigs) {
        this.proxyConfigs = proxyConfigs;
    }

    public Set<LdapConfigResponse> getLdapConfigs() {
        return ldapConfigs;
    }

    public void setLdapConfigs(Set<LdapConfigResponse> ldapConfigs) {
        this.ldapConfigs = ldapConfigs;
    }

    public Set<RDSConfigResponse> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfigResponse> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }
}
