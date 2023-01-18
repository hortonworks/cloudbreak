package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AwsNetworkV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsNetworkParameters extends MappableBase {
    @Schema
    private String vpcId;

    @Schema
    private String subnetId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "vpcId", vpcId);
        putIfValueNotNull(map, SUBNET_ID, subnetId);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        vpcId = getParameterOrNull(parameters, "vpcId");
        subnetId = getParameterOrNull(parameters, SUBNET_ID);
    }

    @Override
    public String toString() {
        return "AwsNetworkParameters{"
                + "vpcId='" + vpcId + '\''
                + ", subnetId='" + subnetId + '\''
                + '}';
    }
}
