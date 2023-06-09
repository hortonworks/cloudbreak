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

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;

@Entity
@DiscriminatorValue("GCP")
public class GcpNetwork extends BaseNetwork {

    private String networkId;

    private String sharedProjectId;

    private Boolean noPublicIp;

    private Boolean noFirewallRules;

    @Convert(converter = JsonToString.class)
    @Column(name = "zonemetas", columnDefinition = "TEXT")
    private Json zoneMetas;

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getSharedProjectId() {
        return sharedProjectId;
    }

    public void setSharedProjectId(String sharedProjectId) {
        this.sharedProjectId = sharedProjectId;
    }

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public Boolean getNoFirewallRules() {
        return noFirewallRules;
    }

    public void setNoFirewallRules(Boolean noFirewallRules) {
        this.noFirewallRules = noFirewallRules;
    }

    @Override
    public String getNetworkId() {
        return networkId;
    }

    public Json getZoneMetas() {
        return zoneMetas;
    }

    public void setZoneMetas(Json zoneMetas) {
        this.zoneMetas = zoneMetas;
    }

    public Set<String> getAvailabilityZones() {
        Set<String> zoneList = new HashSet<>();
        Json zoneMetasJson = getZoneMetas();
        if (zoneMetasJson != null) {
            zoneList.addAll((List<String>) zoneMetasJson
                    .getMap()
                    .getOrDefault(NetworkConstants.AVAILABILITY_ZONES, new ArrayList<>()));
        }
        return zoneList;
    }

    public void setAvailabilityZones(Set<String> zones) {
        if (CollectionUtils.isNotEmpty(zones)) {
            Map<String, Object> existingAttributes = (zoneMetas != null) ? zoneMetas.getMap() : new HashMap<>();
            existingAttributes.put(NetworkConstants.AVAILABILITY_ZONES, zones);
            setZoneMetas(new Json(existingAttributes));
        }
    }

    @Override
    public String toString() {
        String zoneMetasAsString = zoneMetas != null ? zoneMetas.toString() : "";
        return super.toString() + ", " + "GcpNetwork{" +
                "networkId='" + networkId + '\'' +
                ", sharedProjectId='" + sharedProjectId + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", noFirewallRules=" + noFirewallRules +
                ", zoneMetas='" + zoneMetasAsString + '\'' +
                '}';
    }
}
