package com.sequenceiq.environment.api.v1.environment.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Size;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "EnvironmentNetworkGcpV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkGcpParams implements Serializable {

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.GCP_NETWORK_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String networkId;

    @Schema(description = EnvironmentModelDescription.GCP_SHARED_PROJECT_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String sharedProjectId;

    @Schema(description = EnvironmentModelDescription.GCP_NO_PUBLIC_IP, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean noPublicIp = Boolean.FALSE;

    @Schema(description = EnvironmentModelDescription.GCP_NO_FIREWALL_RULES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean noFirewallRules = Boolean.FALSE;

    @Schema(description = EnvironmentModelDescription.GCP_AVAILABILITY_ZONES)
    private Set<String> availabilityZones = new HashSet<>();

    public String getNetworkId() {
        return networkId;
    }

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

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Set<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    @Override
    public String toString() {
        return "EnvironmentNetworkGcpParams{" +
                "networkId='" + networkId + '\'' +
                ", sharedProjectId='" + sharedProjectId + '\'' +
                ", noPublicIp=" + noPublicIp +
                ", noFirewallRules=" + noFirewallRules +
                ", availabilityZones=[" + String.join(",", availabilityZones) + "]" +
                '}';
    }

    public static final class EnvironmentNetworkGcpParamsBuilder {
        private String networkId;

        private String sharedProjectId;

        private Boolean noPublicIp = Boolean.FALSE;

        private Boolean noFirewallRules = Boolean.FALSE;

        private final Set<String> availabilityZones = new HashSet<>();

        private EnvironmentNetworkGcpParamsBuilder() {
        }

        public static EnvironmentNetworkGcpParamsBuilder anEnvironmentNetworkGcpParamsBuilder() {
            return new EnvironmentNetworkGcpParamsBuilder();
        }

        public EnvironmentNetworkGcpParamsBuilder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public EnvironmentNetworkGcpParamsBuilder withSharedProjectId(String sharedProjectId) {
            this.sharedProjectId = sharedProjectId;
            return this;
        }

        public EnvironmentNetworkGcpParamsBuilder withNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public EnvironmentNetworkGcpParamsBuilder withNoFirewallRules(Boolean noFirewallRules) {
            this.noFirewallRules = noFirewallRules;
            return this;
        }

        public EnvironmentNetworkGcpParamsBuilder withAvailabilityZones(Set<String> availabilityZones) {
            if (CollectionUtils.isNotEmpty(availabilityZones)) {
                this.availabilityZones.addAll(availabilityZones);
            }
            return this;
        }

        public EnvironmentNetworkGcpParams build() {
            EnvironmentNetworkGcpParams environmentNetworkGcpParams = new EnvironmentNetworkGcpParams();
            environmentNetworkGcpParams.setNetworkId(networkId);
            environmentNetworkGcpParams.setSharedProjectId(sharedProjectId);
            environmentNetworkGcpParams.setNoPublicIp(noPublicIp);
            environmentNetworkGcpParams.setNoFirewallRules(noFirewallRules);
            environmentNetworkGcpParams.setAvailabilityZones(availabilityZones);
            return environmentNetworkGcpParams;
        }
    }
}
