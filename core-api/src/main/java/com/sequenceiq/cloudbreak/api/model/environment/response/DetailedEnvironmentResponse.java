package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIG_NAMES)
    private Set<ProxyConfigResponse> proxyConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS_NAMES)
    private Set<LdapConfigResponse> ldapConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIG_NAMES)
    private Set<RDSConfigResponse> rdsConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTER_NAMES)
    private Set<StackViewResponse> workloadClusters = new HashSet<>();

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

    public Set<StackViewResponse> getWorkloadClusters() {
        return workloadClusters;
    }

    public void setWorkloadClusters(Set<StackViewResponse> workloadClusters) {
        this.workloadClusters = workloadClusters;
    }
}
