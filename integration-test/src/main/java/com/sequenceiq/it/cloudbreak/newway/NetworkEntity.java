package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;

public class NetworkEntity extends AbstractCloudbreakEntity<NetworkRequest, NetworkResponse, NetworkEntity> {
    public static final String NETWORK = "NETWORK";

    NetworkEntity(String newId) {
        super(newId);
        setRequest(new NetworkRequest());
    }

    NetworkEntity() {
        this(NETWORK);
    }

    public NetworkEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public NetworkEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public NetworkEntity withCloudPlatform(String cloudPlatform) {
        getRequest().setCloudPlatform(cloudPlatform);
        return this;
    }

    public NetworkEntity withParameters(Map<String, Object> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public NetworkEntity withSubnetCIDR(String subnetCIDR) {
        getRequest().setSubnetCIDR(subnetCIDR);
        return this;
    }

    public NetworkEntity withTopologyId(Long topologyId) {
        getRequest().setTopologyId(topologyId);
        return this;
    }
}
