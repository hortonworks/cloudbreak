package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class JsonParameters extends MappableBase {

    @NotNull
    @ApiModelProperty(required = true)
    private String credentialJson;

    public String getCredentialJson() {
        return credentialJson;
    }

    public void setCredentialJson(String credentialJson) {
        this.credentialJson = credentialJson;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("credentialJson", credentialJson);
        map.put("selector", GcpSelectorType.JSON.getName());
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
        super.parse(parameters);
        credentialJson = getParameterOrNull(parameters, "credentialJson");
    }

}
