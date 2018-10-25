package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIG_NAMES)
    private Set<String> proxyConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS_NAMES)
    private Set<String> ldapConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIG_NAMES)
    private Set<String> rdsConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTER_NAMES)
    private Set<String> workloadClusters = new HashSet<>();

    public Set<String> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<String> proxyConfigs) {
        this.proxyConfigs = proxyConfigs == null ? new HashSet<>() : proxyConfigs;
    }

    public Set<String> getLdapConfigs() {
        return ldapConfigs;
    }

    public void setLdapConfigs(Set<String> ldapConfigs) {
        this.ldapConfigs = ldapConfigs == null ? new HashSet<>() : ldapConfigs;
    }

    public Set<String> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<String> rdsConfigs) {
        this.rdsConfigs = rdsConfigs == null ? new HashSet<>() : rdsConfigs;
    }

    public Set<String> getWorkloadClusters() {
        return workloadClusters;
    }

    public void setWorkloadClusters(Set<String> workloadClusters) {
        this.workloadClusters = workloadClusters == null ? new HashSet<>() : workloadClusters;
    }
}
