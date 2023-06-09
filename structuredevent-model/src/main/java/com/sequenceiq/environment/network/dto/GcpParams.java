package com.sequenceiq.environment.network.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = GcpParams.Builder.class)
public class GcpParams {

    private final String networkId;

    private final String sharedProjectId;

    private final Boolean noPublicIp;

    private final Boolean noFirewallRules;

    private final Set<String> availabilityZones;

    private GcpParams(Builder builder) {
        networkId = builder.networkId;
        sharedProjectId = builder.sharedProjectId;
        noPublicIp = builder.noPublicIp;
        noFirewallRules = builder.noFirewallRules;
        availabilityZones = builder.availabilityZones;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getSharedProjectId() {
        return sharedProjectId;
    }

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public Boolean getNoFirewallRules() {
        return noFirewallRules;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpParams{" +
                "networkId='" + networkId + '\'' +
                ", sharedProjectId='" + sharedProjectId + '\'' +
                ", noFirewallRules='" + noFirewallRules + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", availabilityZones=[" + String.join(",", availabilityZones) + "]" +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String networkId;

        private String sharedProjectId;

        private Boolean noPublicIp;

        private Boolean noFirewallRules;

        private final Set<String> availabilityZones = new HashSet<>();

        private Builder() {
        }

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withSharedProjectId(String sharedProjectId) {
            this.sharedProjectId = sharedProjectId;
            return this;
        }

        public Builder withNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public Builder withNoFirewallRules(Boolean noFirewallRules) {
            this.noFirewallRules = noFirewallRules;
            return this;
        }

        public Builder withAvailabilityZones(Set<String> availabilityZones) {
            if (CollectionUtils.isNotEmpty(availabilityZones)) {
                this.availabilityZones.addAll(availabilityZones);
            }
            return this;
        }

        public GcpParams build() {
            return new GcpParams(this);
        }
    }
}
