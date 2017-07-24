package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNetworksResponse implements JsonEntity {

    private Map<String, Set<PlatformNetworkResponse>> networks = new HashMap<>();

    public PlatformNetworksResponse(Map<String, Set<PlatformNetworkResponse>> networks) {
        this.networks = networks;
    }

    public Map<String, Set<PlatformNetworkResponse>> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Set<PlatformNetworkResponse>> networks) {
        this.networks = networks;
    }
}
