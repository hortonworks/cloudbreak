package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class AwsNetwork extends Network {

    private String vpcId;
    private String internetGatewayId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

    public boolean isExistingVPC() {
        return super.getSubnetCIDR() != null
                && vpcId != null
                && internetGatewayId != null;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
