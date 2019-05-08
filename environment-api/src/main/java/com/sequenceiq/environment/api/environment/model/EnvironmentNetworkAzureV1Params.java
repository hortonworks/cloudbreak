package com.sequenceiq.environment.api.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkAzureV1Params {
    @ApiModelProperty(value = EnvironmentModelDescription.AZURE_NETWORK_ID, required = true)
    private String networkId;

    @ApiModelProperty(value = EnvironmentModelDescription.AZURE_RESOURCE_GROUP_NAME, required = true)
    private String resourceGroupName;

    @ApiModelProperty(EnvironmentModelDescription.AZURE_NO_PUBLIC_IP)
    private boolean noPublicIp;

    @ApiModelProperty(EnvironmentModelDescription.AZURE_NO_FIREWALL_RULES)
    private boolean noFirewallRules;

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

    public boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public boolean getNoFirewallRules() {
        return noFirewallRules;
    }

    public void setNoFirewallRules(boolean noFirewallRules) {
        this.noFirewallRules = noFirewallRules;
    }
}
