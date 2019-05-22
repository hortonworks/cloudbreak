package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class EnvironmentV4BaseRequest {

    @ApiModelProperty(EnvironmentRequestModelDescription.PROXY_CONFIGS)
    private Set<String> proxies = new HashSet<>();

    @ApiModelProperty(EnvironmentRequestModelDescription.LDAP_CONFIGS)
    private Set<String> ldaps = new HashSet<>();

    @ApiModelProperty(EnvironmentRequestModelDescription.RDS_CONFIGS)
    private Set<String> databases = new HashSet<>();

    @ApiModelProperty(EnvironmentRequestModelDescription.KUBERNETES_CONFIGS)
    private Set<String> kubernetes = new HashSet<>();

    public Set<String> getProxies() {
        return proxies;
    }

    public void setProxies(Set<String> proxies) {
        this.proxies = proxies == null ? new HashSet<>() : proxies;
    }

    public Set<String> getLdaps() {
        return ldaps;
    }

    public void setLdaps(Set<String> ldaps) {
        this.ldaps = ldaps == null ? new HashSet<>() : ldaps;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<String> databases) {
        this.databases = databases == null ? new HashSet<>() : databases;
    }

    public Set<String> getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(Set<String> kubernetes) {
        this.kubernetes = kubernetes == null ? new HashSet<>() : kubernetes;
    }

}
