package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SimpleEnvironmentResponse extends EnvironmentBaseResponse {

    @ApiModelProperty(EnvironmentResponseModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIG_NAMES)
    private Set<String> proxyConfigs;

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS_NAMES)
    private Set<String> ldapConfigs;

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIG_NAMES)
    private Set<String> rdsConfigs;

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public Set<String> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<String> proxyConfigs) {
        this.proxyConfigs = proxyConfigs;
    }

    public Set<String> getLdapConfigs() {
        return ldapConfigs;
    }

    public void setLdapConfigs(Set<String> ldapConfigs) {
        this.ldapConfigs = ldapConfigs;
    }

    public Set<String> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<String> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }
}
