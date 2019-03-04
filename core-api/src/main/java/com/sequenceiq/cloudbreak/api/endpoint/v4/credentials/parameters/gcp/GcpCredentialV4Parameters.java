package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp;

import java.util.Map;

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
public class GcpCredentialV4Parameters extends MappableBase {

    @ApiModelProperty
    private P12Parameters p12;

    @ApiModelProperty
    private JsonParameters json;

    public P12Parameters getP12() {
        return p12;
    }

    public JsonParameters getJson() {
        return json;
    }

    public void setP12(P12Parameters p12) {
        this.p12 = p12;
    }

    public void setJson(JsonParameters json) {
        this.json = json;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        if (p12 != null) {
            map.putAll(p12.asMap());
        } else if (json != null) {
            map.putAll(json.asMap());
        }
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
        p12 = new P12Parameters();
        p12.parse(parameters);
        json = new JsonParameters();
        json.parse(parameters);
    }

}
