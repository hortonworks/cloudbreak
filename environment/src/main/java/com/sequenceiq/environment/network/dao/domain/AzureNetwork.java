package com.sequenceiq.environment.network.dao.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AZURE")
public class AzureNetwork extends BaseNetwork {
    private String networkId;

    private String resourceGroupName;

    private Boolean noPublicIp;

    private String privateDnsZoneId;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public Boolean getNoPublicIp() {
        return noPublicIp == null ? Boolean.FALSE : noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public String getPrivateDnsZoneId() {
        return privateDnsZoneId;
    }

    public void setPrivateDnsZoneId(String privateDnsZoneId) {
        this.privateDnsZoneId = privateDnsZoneId;
    }

    @Override
    public String toString() {
        return "AzureNetwork{" +
                "networkId='" + networkId + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", privateDnsZoneId='" + privateDnsZoneId + '\'' +
                "} " + super.toString();
    }
}
