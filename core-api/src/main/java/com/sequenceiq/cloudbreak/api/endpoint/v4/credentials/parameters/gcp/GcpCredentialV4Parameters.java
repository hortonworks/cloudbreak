package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcpCredentialV4Parameters implements CredentialV4Parameters {

    @ApiModelProperty
    private P12Parameters p12;

    @ApiModelProperty
    private JsonParameters json;

    public P12Parameters getP12() {
        return p12;
    }

    public void setP12(P12Parameters p12) {
        this.p12 = p12;
    }

    public JsonParameters getJson() {
        return json;
    }

    public void setJson(JsonParameters json) {
        this.json = json;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (p12 != null) {
            map.putAll(p12.asMap());
        } else if (json != null) {
            map.putAll(json.asMap());
        }
        return map;
    }

}
