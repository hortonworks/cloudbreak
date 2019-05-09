package com.sequenceiq.environment.api.credential.model.parameters.gcp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpCredentialV1Parameters implements Serializable {

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
}
