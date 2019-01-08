package com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses;

import io.swagger.annotations.ApiModel;

import java.util.HashSet;
import java.util.Set;

@ApiModel
public class ProxyV4Responses {
    private Set<ProxyV4Response> proxies = new HashSet<>();

    public Set<ProxyV4Response> getProxies() {
        return proxies;
    }

    public void setProxies(Set<ProxyV4Response> proxies) {
        this.proxies = proxies;
    }

    public static final ProxyV4Responses proxyV4Responses(Set<ProxyV4Response> proxies) {
        ProxyV4Responses proxyV4Responses = new ProxyV4Responses();
        proxyV4Responses.setProxies(proxies);
        return proxyV4Responses;
    }
}
