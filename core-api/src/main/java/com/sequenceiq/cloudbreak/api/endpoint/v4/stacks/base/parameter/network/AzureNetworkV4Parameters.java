package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.AKS_PRIVATE_DNS_ZONE_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.DATABASE_PRIVATE_DNS_ZONE_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NO_PUBLIC_IP;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.SUBNET_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureNetworkV4Parameters extends MappableBase implements JsonEntity {

    @ApiModelProperty
    private Boolean noPublicIp;

    @ApiModelProperty
    private String resourceGroupName;

    @ApiModelProperty
    private String networkId;

    @ApiModelProperty
    private String subnetId;

    @ApiModelProperty
    private String databasePrivateDnsZoneId;

    @ApiModelProperty
    private String aksPrivateDnsZoneId;

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
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

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, NO_PUBLIC_IP, noPublicIp);
        putIfValueNotNull(map, RESOURCE_GROUP_NAME, resourceGroupName);
        putIfValueNotNull(map, NETWORK_ID, networkId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        putIfValueNotNull(map, DATABASE_PRIVATE_DNS_ZONE_ID, databasePrivateDnsZoneId);
        putIfValueNotNull(map, AKS_PRIVATE_DNS_ZONE_ID, aksPrivateDnsZoneId);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        noPublicIp = getBoolean(parameters, NO_PUBLIC_IP);
        resourceGroupName = getParameterOrNull(parameters, RESOURCE_GROUP_NAME);
        networkId = getParameterOrNull(parameters, NETWORK_ID);
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
        databasePrivateDnsZoneId = getParameterOrNull(parameters, DATABASE_PRIVATE_DNS_ZONE_ID);
        aksPrivateDnsZoneId = getParameterOrNull(parameters, AKS_PRIVATE_DNS_ZONE_ID);
    }
}
