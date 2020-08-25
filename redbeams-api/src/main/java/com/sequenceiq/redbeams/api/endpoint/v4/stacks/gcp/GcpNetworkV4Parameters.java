package com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.GcpNetworkModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpNetworkV4Parameters extends MappableBase {

    private static final String SUBNETS = "subnets";

    @ApiModelProperty(GcpNetworkModelDescription.SUBNETS)
    private String subnets;

    public String getSubnets() {
        return subnets;
    }

    public void setSubnets(String subnets) {
        this.subnets = subnets;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, SUBNETS, subnets);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        subnets = getParameterOrNull(parameters, SUBNETS);
    }
}
