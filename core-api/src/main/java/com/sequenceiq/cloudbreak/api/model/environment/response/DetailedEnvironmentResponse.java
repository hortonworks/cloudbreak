package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.KubernetesConfigResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIGS)
    private Set<ProxyConfigResponse> proxyConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS)
    private Set<LdapConfigResponse> ldapConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIGS)
    private Set<RDSConfigResponse> rdsConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KUBERNETES_CONFIGS)
    private Set<KubernetesConfigResponse> kubernetesConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KERBEROS_CONFIGS)
    private Set<KerberosResponse> kerberosConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_RESOURCES)
    private Set<DatalakeResourcesResponse> datalakeResourcesResponses;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTERS)
    private Set<StackViewResponse> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTERS)
    private Set<StackViewResponse> datalakeClusters = new HashSet<>();

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

    public Set<KubernetesConfigResponse> getKubernetesConfigs() {
        return kubernetesConfigs;
    }

    public void setKubernetesConfigs(Set<KubernetesConfigResponse> kubernetesConfigs) {
        this.kubernetesConfigs = kubernetesConfigs;
    }

    public Set<DatalakeResourcesResponse> getDatalakeResourcesResponses() {
        return datalakeResourcesResponses;
    }

    public void setDatalakeResourcesResponses(Set<DatalakeResourcesResponse> datalakeResourcesResponses) {
        this.datalakeResourcesResponses = datalakeResourcesResponses;
    }

    public Set<StackViewResponse> getWorkloadClusters() {
        return workloadClusters;
    }

    public void setWorkloadClusters(Set<StackViewResponse> workloadClusters) {
        this.workloadClusters = workloadClusters;
    }

    public Set<StackViewResponse> getDatalakeClusters() {
        return datalakeClusters;
    }

    public void setDatalakeClusters(Set<StackViewResponse> datalakeClusters) {
        this.datalakeClusters = datalakeClusters;
    }

    public Set<KerberosResponse> getKerberosConfigs() {
        return kerberosConfigs;
    }

    public void setKerberosConfigs(Set<KerberosResponse> kerberosConfigs) {
        this.kerberosConfigs = kerberosConfigs;
    }
}
