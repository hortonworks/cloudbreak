package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNetworksResponse implements Serializable {

    private Map<String, Set<PlatformNetworkResponse>> networks = new HashMap<>();

    public PlatformNetworksResponse() {
    }

    public PlatformNetworksResponse(Map<String, Set<PlatformNetworkResponse>> networks) {
        this.networks = networks;
    }

    public Map<String, Set<PlatformNetworkResponse>> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Set<PlatformNetworkResponse>> networks) {
        this.networks = networks;
    }

    @Override
    public String toString() {
        return "PlatformNetworksResponse{" +
                "networks=" + networks +
                '}';
    }
}
