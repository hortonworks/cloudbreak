package com.sequenceiq.cloudbreak.domain;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Entity
public class AzureNetwork extends Network {

    private String addressPrefixCIDR;

    public String getAddressPrefixCIDR() {
        return addressPrefixCIDR;
    }

    public void setAddressPrefixCIDR(String addressPrefixCIDR) {
        this.addressPrefixCIDR = addressPrefixCIDR;
    }

    @Override
    public List<CloudPlatform> cloudPlatform() {
        return Arrays.asList(CloudPlatform.AZURE, CloudPlatform.AZURE_RM);
    }
}
