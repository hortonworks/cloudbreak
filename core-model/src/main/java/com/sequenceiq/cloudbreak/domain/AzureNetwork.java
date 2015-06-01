package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

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
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
