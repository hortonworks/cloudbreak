package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws;

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
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceGroupAwsNetworkV4Parameters extends MappableBase implements JsonEntity {

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
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, NetworkConstants.SUBNET_IDS, subnetIds);
        putIfValueNotNull(map, NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS, endpointGatewaySubnetIds);
        return map;
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
}
