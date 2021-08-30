package com.sequenceiq.distrox.api.v1.distrox.model.network.aws;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceGroupAwsNetworkV1Parameters extends MappableBase implements Serializable {

    @ApiModelProperty
    private List<String> subnetIds = new ArrayList<>();

    @ApiModelProperty
    private List<String> endpointGatewaySubnetIds = new ArrayList<>();

    public List<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public List<String> getEndpointGatewaySubnetIds() {
        return endpointGatewaySubnetIds;
    }

    public void setEndpointGatewaySubnetIds(List<String> endpointGatewaySubnetIds) {
        this.endpointGatewaySubnetIds = endpointGatewaySubnetIds;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        subnetIds = getStringList(parameters, NetworkConstants.SUBNET_IDS);
        endpointGatewaySubnetIds = getStringList(parameters, NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS);
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put(NetworkConstants.SUBNET_IDS, subnetIds);
        map.put(NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS, endpointGatewaySubnetIds);
        return map;
    }
}
