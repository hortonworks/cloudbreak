package com.sequenceiq.environment.network.dao.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.constant.AzureConstants;

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
    @Column(columnDefinition = "TEXT")
    private Json zonemetas;

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

    public Json getZonemetas() {
        return zonemetas;
    }

    public void setZonemetas(Json zonemetas) {
        this.zonemetas = zonemetas;
    }

    public Set<String> getAvailabilityZones() {
        Set<String> zoneList = new HashSet<>();
        if (zonemetas != null) {
            zoneList.addAll((List<String>) zonemetas
                    .getMap()
                    .getOrDefault(AzureConstants.ZONES, new ArrayList<>()));
        }
        return zoneList;
    }

    public void setAvailabilityZones(Set<String> zones) {
        if (CollectionUtils.isEmpty(zones)) {
            return;
        }
        Map<String, Object> existingAttributes = (zonemetas != null) ? zonemetas.getMap() : new HashMap<>();
        existingAttributes.put(AzureConstants.ZONES, zones);
        zonemetas = new Json(existingAttributes);
    }

    @Override
    public String toString() {
        return "AzureNetwork{" +
                "networkId='" + networkId + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", databasePrivateDnsZoneId='" + databasePrivateDnsZoneId + '\'' +
                ", aksPrivateDnsZoneId='" + aksPrivateDnsZoneId + '\'' +
                ", noOutboundLoadBalancer='" + noOutboundLoadBalancer + '\'' +
                ", zones='" + zonemetas + '\'' +
                "} " + super.toString();
    }
}
