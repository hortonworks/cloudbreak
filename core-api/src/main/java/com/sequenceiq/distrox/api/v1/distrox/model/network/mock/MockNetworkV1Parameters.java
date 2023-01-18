package com.sequenceiq.distrox.api.v1.distrox.model.network.mock;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockNetworkV1Parameters implements Serializable {

    /**
     * @deprecated should not be used anymore
     */
    @Schema
    @Deprecated
    private String subnetId;

    /**
     * @deprecated should not be used anymore
     */
    @Schema
    @Deprecated
    private String internetGatewayId;

    @Schema
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

    @JsonIgnore
    public List<String> getSubnetIds() {
        return Arrays.asList(subnetId);
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

}
