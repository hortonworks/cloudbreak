package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.AzureNetworkModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureNetworkV4Parameters extends MappableBase {

    private static final String SUBNETS = "subnets";

    @Schema(description = AzureNetworkModelDescription.SUBNETS)
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
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        subnets = getParameterOrNull(parameters, SUBNETS);
    }

    @Override
    public String toString() {
        return "AzureNetworkV4Parameters{" +
                "subnets='" + subnets + '\'' +
                '}';
    }
}
