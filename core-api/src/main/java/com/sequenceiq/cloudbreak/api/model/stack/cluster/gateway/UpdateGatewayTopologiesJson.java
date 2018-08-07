package com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway;

import java.util.List;

public class UpdateGatewayTopologiesJson {

    private List<GatewayTopologyJson> topologies;

    public List<GatewayTopologyJson> getTopologies() {
        return topologies;
    }

    public void setTopologies(List<GatewayTopologyJson> topologies) {
        this.topologies = topologies;
    }
}
