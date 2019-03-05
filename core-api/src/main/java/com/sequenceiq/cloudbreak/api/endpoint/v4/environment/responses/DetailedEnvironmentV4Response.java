package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DetailedEnvironmentV4Response extends EnvironmentV4BaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIGS)
    private Set<ProxyV4Response> proxies = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS)
    private Set<LdapV4Response> ldaps = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIGS)
    private Set<DatabaseV4Response> databases = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KUBERNETES_CONFIGS)
    private Set<KubernetesV4Response> kubernetes = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KERBEROS_CONFIGS)
    private Set<KerberosV4Response> kerberoses = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_RESOURCES)
    private Set<DatalakeResourcesV4Response> datalakeResourcesResponses;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTERS)
    private Set<StackViewV4Response> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTERS)
    private Set<StackViewV4Response> datalakeClusters = new HashSet<>();

    public Set<ProxyV4Response> getProxies() {
        return proxies;
    }

    public void setProxies(Set<ProxyV4Response> proxies) {
        this.proxies = proxies;
    }

    public Set<LdapV4Response> getLdaps() {
        return ldaps;
    }

    public void setLdaps(Set<LdapV4Response> ldaps) {
        this.ldaps = ldaps;
    }

    public Set<DatabaseV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<DatabaseV4Response> databases) {
        this.databases = databases;
    }

    public Set<KubernetesV4Response> getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(Set<KubernetesV4Response> kubernetes) {
        this.kubernetes = kubernetes;
    }

    public Set<DatalakeResourcesV4Response> getDatalakeResources() {
        return datalakeResourcesResponses;
    }

    public void setDatalakeResources(Set<DatalakeResourcesV4Response> datalakeResources) {
        datalakeResourcesResponses = datalakeResources;
    }

    public Set<StackViewV4Response> getWorkloadClusters() {
        return workloadClusters;
    }

    public void setWorkloadClusters(Set<StackViewV4Response> workloadClusters) {
        this.workloadClusters = workloadClusters;
    }

    public Set<StackViewV4Response> getDatalakeClusters() {
        return datalakeClusters;
    }

    public void setDatalakeClusters(Set<StackViewV4Response> datalakeClusters) {
        this.datalakeClusters = datalakeClusters;
    }

    public Set<KerberosV4Response> getKerberoses() {
        return kerberoses;
    }

    public void setKerberoses(Set<KerberosV4Response> kerberoses) {
        this.kerberoses = kerberoses;
    }
}
