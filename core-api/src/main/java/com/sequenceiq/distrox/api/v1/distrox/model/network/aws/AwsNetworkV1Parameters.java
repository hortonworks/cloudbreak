package com.sequenceiq.distrox.api.v1.distrox.model.network.aws;

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
public class AwsNetworkV1Parameters implements Serializable {

    /**
     * @deprecated should not be used anymore
     */
    @Schema
    @Deprecated
    private String subnetId;

    @Schema
    private AwsLoadBalancerParams loadBalancer;

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @JsonIgnore
    public List<String> getSubnetIds() {
        return Arrays.asList(subnetId);
    }

    public AwsLoadBalancerParams getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(AwsLoadBalancerParams loadBalancer) {
        this.loadBalancer = loadBalancer;
    }
}
