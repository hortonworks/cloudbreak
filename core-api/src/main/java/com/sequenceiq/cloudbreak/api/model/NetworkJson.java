package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkJson implements JsonEntity {
    @ApiModelProperty(value = ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @Size(max = 100, min = 1, message = "The length of the network's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The network's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String name;
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    @Size(max = 1000)
    private String description;
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, required = true)
    @NotNull
    private boolean publicInAccount;
    @ApiModelProperty(value = ModelDescriptions.NetworkModelDescription.SUBNET_CIDR)
    @ValidSubnet
    private String subnetCIDR;
    @ApiModelProperty(value = ModelDescriptions.NetworkModelDescription.PARAMETERS, required = true)
    private Map<String, Object> parameters = new HashMap<>();
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    @NotNull
    private String cloudPlatform;
    @ApiModelProperty(value = ModelDescriptions.TOPOLOGY_ID)
    private Long topologyId;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

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

    @JsonProperty("publicInAccount")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    @JsonIgnore
    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
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
