package com.sequenceiq.environment.api.v1.environment.model;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
@ApiModel(value = "EnvironmentNetworkGcpV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkGcpParams {

    @Size(max = 255)
    @ApiModelProperty(value = EnvironmentModelDescription.GCP_NETWORK_ID, required = true)
    private String networkId;

    @ApiModelProperty(value = EnvironmentModelDescription.GCP_SHARED_PROJECT_ID, required = true)
    private String sharedProjectId;

    @ApiModelProperty(value = EnvironmentModelDescription.GCP_NO_PUBLIC_IP, required = true)
    private Boolean noPublicIp;

    @ApiModelProperty(value = EnvironmentModelDescription.GCP_NO_FIREWALL_RULES, required = true)
    private Boolean noFirewallRules;

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

    public static final class EnvironmentNetworkGcpParamsBuilder {
        private String networkId;

        private String sharedProjectId;

        private Boolean noPublicIp;

        private Boolean noFirewallRules;

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

        public EnvironmentNetworkGcpParams build() {
            EnvironmentNetworkGcpParams environmentNetworkGcpParams = new EnvironmentNetworkGcpParams();
            environmentNetworkGcpParams.setNetworkId(networkId);
            environmentNetworkGcpParams.setSharedProjectId(sharedProjectId);
            environmentNetworkGcpParams.setNoPublicIp(noPublicIp);
            environmentNetworkGcpParams.setNoFirewallRules(noFirewallRules);
            return environmentNetworkGcpParams;
        }
    }
}
