package com.sequenceiq.environment.network.dao.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@DiscriminatorValue("AWS")
public class AwsNetwork extends BaseNetwork {
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
        return super.toString() + ", " + "AwsNetwork{" +
                "vpcId='" + vpcId + '\'' +
                '}';
    }
}
