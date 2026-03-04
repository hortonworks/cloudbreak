package com.sequenceiq.environment.network.dao.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("OPENSTACK")
public class OpenstackNetwork extends BaseNetwork {

    private String networkId;

    private String routerId;

    private String publicNetId;

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    @Override
    public String getNetworkId() {
        return networkId;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    @Override
    public String toString() {
        return "OpenstackNetwork{" +
                "networkId='" + networkId + '\'' +
                ", routerId='" + routerId + '\'' +
                ", publicNetId='" + publicNetId + '\'' +
                "} " + super.toString();
    }
}