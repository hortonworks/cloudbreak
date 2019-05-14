package com.sequenceiq.environment.api.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentV1Response extends EnvironmentV1BaseResponse {
    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_RESPONSE)
    private Set<ProxyV1Response> proxies = new HashSet<>();

//    @ApiModelProperty(EnvironmentModelDescription.LDAP_CONFIGS_RESPONSE)
//    private final Set<LdapV1Response> ldaps = new HashSet<>();

//    @ApiModelProperty(EnvironmentModelDescription.WORKLOAD_CLUSTERS)
//    private final Set<String> workloadClusters = new HashSet<>();
//
//    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_CLUSTERS)
//    private final Set<String> datalakeClusters = new HashSet<>();

//    public Set<String> getWorkloadClusters() {
//        return workloadClusters;
//    }

//    public Set<String> getDatalakeClusters() {
//        return datalakeClusters;
//    }

    public void setProxies(Set<ProxyV1Response> proxies) {
        this.proxies = proxies;
    }

    public Set<ProxyV1Response> getProxies() {
        return proxies;
    }
}
