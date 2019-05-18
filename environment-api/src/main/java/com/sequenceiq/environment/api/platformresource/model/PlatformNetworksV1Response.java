package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNetworksV1Response implements Serializable {

    private Map<String, Set<PlatformNetworkV1Response>> networks = new HashMap<>();

    public PlatformNetworksV1Response() {
    }

    public PlatformNetworksV1Response(Map<String, Set<PlatformNetworkV1Response>> networks) {
        this.networks = networks;
    }

    public Map<String, Set<PlatformNetworkV1Response>> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Set<PlatformNetworkV1Response>> networks) {
        this.networks = networks;
    }
}
