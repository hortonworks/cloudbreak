package com.sequenceiq.environment.api.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.LdapV4Response;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentV1Response extends EnvironmentV1BaseResponse {
    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_RESPONSE)
    private final Set<ProxyV1Response> proxies = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.LDAP_CONFIGS_RESPONSE)
    private final Set<LdapV4Response> ldaps = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.RDS_CONFIGS_RESPONSE)
    private final Set<String> databases = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.KUBERNETES_CONFIGS_RESPONSE)
    private final Set<String> kubernetes = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.KERBEROS_CONFIGS_RESPONSE)
    private final Set<String> kerberoses = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_RESOURCES)
    private Set<DatalakeResourcesV1Response> datalakeResourcesResponses;

    @ApiModelProperty(EnvironmentModelDescription.WORKLOAD_CLUSTERS)
    private final Set<String> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_CLUSTERS)
    private final Set<String> datalakeClusters = new HashSet<>();

    public Set<ProxyV1Response> getProxies() {
        return proxies;
    }

    public Set<LdapV4Response> getLdaps() {
        return ldaps;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public Set<String> getKubernetes() {
        return kubernetes;
    }

    public Set<String> getKerberoses() {
        return kerberoses;
    }

    public Set<DatalakeResourcesV1Response> getDatalakeResourcesResponses() {
        return datalakeResourcesResponses;
    }

    public void setDatalakeResourcesResponses(Set<DatalakeResourcesV1Response> datalakeResourcesResponses) {
        this.datalakeResourcesResponses = datalakeResourcesResponses;
    }

    public Set<String> getWorkloadClusters() {
        return workloadClusters;
    }

    public Set<String> getDatalakeClusters() {
        return datalakeClusters;
    }
}
