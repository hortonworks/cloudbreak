package com.sequenceiq.environment.network.dao.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("GCP")
public class GcpNetwork extends BaseNetwork {

    private String networkId;

    private String sharedProjectId;

    private Boolean noPublicIp;

    private Boolean noFirewallRules;

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

    public String getNetworkId() {
        return networkId;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "GcpNetwork{" +
                "networkId='" + networkId + '\'' +
                ", sharedProjectId='" + sharedProjectId + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", noFirewallRules=" + noFirewallRules +
                '}';
    }
}
