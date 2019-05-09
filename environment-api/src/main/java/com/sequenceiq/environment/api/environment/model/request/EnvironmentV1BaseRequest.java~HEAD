package com.sequenceiq.environment.api.environment.model.request;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class EnvironmentV1BaseRequest {

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_REQUEST)
    private Set<String> proxies = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_REQUEST)
    private Set<String> ldaps = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_REQUEST)
    private Set<String> databases = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_REQUEST)
    private Set<String> kubernetes = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.KERBEROS_CONFIGS_REQUEST)
    private Set<String> kerberoses = new HashSet<>();

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

    public Set<String> getKerberoses() {
        return kerberoses;
    }

    public void setKerberoses(Set<String> kerberoses) {
        this.kerberoses = kerberoses == null ? new HashSet<>() : kerberoses;
    }
}
