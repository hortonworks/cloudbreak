package com.sequenceiq.cloudbreak.domain;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

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
    public List<CloudPlatform> cloudPlatform() {
        return Arrays.asList(CloudPlatform.AWS);
    }
}
