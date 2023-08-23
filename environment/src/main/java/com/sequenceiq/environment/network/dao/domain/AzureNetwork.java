package com.sequenceiq.environment.network.dao.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.database.StringSetToStringConverter;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;

@Entity
@DiscriminatorValue("AZURE")
public class AzureNetwork extends BaseNetwork {
    private String networkId;

    private String resourceGroupName;

    private Boolean noPublicIp;

    private String databasePrivateDnsZoneId;

    private String aksPrivateDnsZoneId;

    private boolean noOutboundLoadBalancer;

    @Convert(converter = JsonToString.class)
    @Column(name = "zonemetas", columnDefinition = "TEXT")
    private Json zoneMetas;

    @Convert(converter = StringSetToStringConverter.class)
    @Column(name = "flexibleserversubnetids", columnDefinition = "TEXT")
    private Set<String> flexibleServerSubnetIds = new HashSet<>();

    @Override
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

    public String getDatabasePrivateDnsZoneId() {
        return databasePrivateDnsZoneId;
    }

    public void setDatabasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
        this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
    }

    public String getAksPrivateDnsZoneId() {
        return aksPrivateDnsZoneId;
    }

    public void setAksPrivateDnsZoneId(String aksPrivateDnsZoneId) {
        this.aksPrivateDnsZoneId = aksPrivateDnsZoneId;
    }

    public boolean isNoOutboundLoadBalancer() {
        return noOutboundLoadBalancer;
    }

    public void setNoOutboundLoadBalancer(boolean noOutboundLoadBalancer) {
        this.noOutboundLoadBalancer = noOutboundLoadBalancer;
    }

    public Json getZoneMetas() {
        return zoneMetas;
    }

    public void setZoneMetas(Json zoneMetas) {
        this.zoneMetas = zoneMetas;
    }

    public Set<String> getFlexibleServerSubnetIds() {
        return flexibleServerSubnetIds;
    }

    public void setFlexibleServerSubnetIds(Set<String> flexibleServerSubnetIds) {
        this.flexibleServerSubnetIds = flexibleServerSubnetIds;
    }

    @Override
    public String toString() {
        return "AzureNetwork{" +
                "networkId='" + networkId + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", databasePrivateDnsZoneId='" + databasePrivateDnsZoneId + '\'' +
                ", aksPrivateDnsZoneId='" + aksPrivateDnsZoneId + '\'' +
                ", noOutboundLoadBalancer=" + noOutboundLoadBalancer +
                ", zoneMetas=" + zoneMetas +
                ", flexibleServerSubnetIds=" + flexibleServerSubnetIds +
                "} " + super.toString();
    }
}
