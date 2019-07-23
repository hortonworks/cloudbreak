package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.AzureNetworkModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureNetworkV4Parameters extends MappableBase {

    private static final String VIRTUAL_NETWORK = "virtualNetwork";

    @ApiModelProperty(AzureNetworkModelDescription.VIRTUAL_NETWORK)
    private String virtualNetwork;

    public String getVirtualNetwork() {
        return virtualNetwork;
    }

    public void setVirtualNetwork(String virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, VIRTUAL_NETWORK, virtualNetwork);
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
        virtualNetwork = getParameterOrNull(parameters, VIRTUAL_NETWORK);
    }
}
