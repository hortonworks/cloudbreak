package com.sequenceiq.distrox.api.v1.distrox.model.network.mock;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockNetworkV1Parameters implements Serializable {

    /**
     * @deprecated should not be used anymore
     */
    @ApiModelProperty
    @Deprecated
    private String subnetId;

    /**
     * @deprecated should not be used anymore
     */
    @ApiModelProperty
    @Deprecated
    private String internetGatewayId;

    @ApiModelProperty
    private String vpcId;

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public List<String> getSubnetIds() {
        return List.of(subnetId);
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

}
