package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIGS)
    private Set<ProxyV4Response> proxyConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS)
    private Set<LdapConfigResponse> ldapConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIGS)
    private Set<DatabaseV4Response> rdsConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KUBERNETES_CONFIGS)
    private Set<KubernetesV4Response> kubernetesConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KERBEROS_CONFIGS)
    private Set<KerberosV4Response> kerberosConfigs = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_RESOURCES)
    private DatalakeResourcesResponse datalakeResourcesResponse;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTERS)
    private Set<StackViewResponse> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTERS)
    private Set<StackViewResponse> datalakeClusters = new HashSet<>();

    public Set<ProxyV4Response> getProxyConfigs() {
        return proxyConfigs;
    }

    public void setProxyConfigs(Set<ProxyV4Response> proxyConfigs) {
        this.proxyConfigs = proxyConfigs;
    }

    public Set<LdapConfigResponse> getLdapConfigs() {
        return ldapConfigs;
    }

    public void setLdapConfigs(Set<LdapConfigResponse> ldapConfigs) {
        this.ldapConfigs = ldapConfigs;
    }

    public Set<DatabaseV4Response> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<DatabaseV4Response> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public Set<KubernetesV4Response> getKubernetesConfigs() {
        return kubernetesConfigs;
    }

    public void setKubernetesConfigs(Set<KubernetesV4Response> kubernetesConfigs) {
        this.kubernetesConfigs = kubernetesConfigs;
    }

    public DatalakeResourcesResponse getDatalakeResourcesResponse() {
        return datalakeResourcesResponse;
    }

    public void setDatalakeResourcesResponse(DatalakeResourcesResponse datalakeResourcesResponse) {
        this.datalakeResourcesResponse = datalakeResourcesResponse;
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

    public Set<KerberosV4Response> getKerberosConfigs() {
        return kerberosConfigs;
    }

    public void setKerberosConfigs(Set<KerberosV4Response> kerberosConfigs) {
        this.kerberosConfigs = kerberosConfigs;
    }
}
