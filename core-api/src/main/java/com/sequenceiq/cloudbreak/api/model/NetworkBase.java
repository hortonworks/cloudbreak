package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class NetworkBase implements JsonEntity {
    @Size(max = 100, min = 1, message = "The length of the network's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The network's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    @Size(max = 1000)
    private String description;
    @ApiModelProperty(value = ModelDescriptions.NetworkModelDescription.SUBNET_CIDR)
    @ValidSubnet
    private String subnetCIDR;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;
    @ApiModelProperty(value = ModelDescriptions.NetworkModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();
    @ApiModelProperty(value = ModelDescriptions.TOPOLOGY_ID)
    private Long topologyId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubnetCIDR() {
        return subnetCIDR;
    }

    public void setSubnetCIDR(String subnetCIDR) {
        this.subnetCIDR = subnetCIDR;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }
}
