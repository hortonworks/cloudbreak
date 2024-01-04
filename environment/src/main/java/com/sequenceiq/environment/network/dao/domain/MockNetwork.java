package com.sequenceiq.environment.network.dao.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@DiscriminatorValue("MOCK")
public class MockNetwork extends BaseNetwork {
    private String vpcId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    @JsonIgnore
    public String getNetworkId() {
        return vpcId;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "MockNetwork{" +
                "vpcId='" + vpcId + '\'' +
                '}';
    }
}
