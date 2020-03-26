package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNetworksV4Response implements JsonEntity {

    private Map<String, Set<PlatformNetworkV4Response>> networks = new HashMap<>();

    public PlatformNetworksV4Response() {
    }

    public PlatformNetworksV4Response(Map<String, Set<PlatformNetworkV4Response>> networks) {
        this.networks = networks;
    }

    public Map<String, Set<PlatformNetworkV4Response>> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, Set<PlatformNetworkV4Response>> networks) {
        this.networks = networks;
    }
}
