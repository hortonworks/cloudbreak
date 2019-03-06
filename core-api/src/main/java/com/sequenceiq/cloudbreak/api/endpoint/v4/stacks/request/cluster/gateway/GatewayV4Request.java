package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.GatewayModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GatewayV4Request implements JsonEntity {

    @JsonIgnore
    @ApiModelProperty(GatewayModelDescription.KNOX_PATH)
    private String path;

    @ApiModelProperty(GatewayModelDescription.GATEWAY_TOPOLOGIES)
    private List<GatewayTopologyV4Request> topologies;

    @JsonIgnore
    @ApiModelProperty(GatewayModelDescription.KNOX_SSO_PROVIDER)
    private String ssoProvider;

    @ApiModelProperty(GatewayModelDescription.KNOX_SSO_CERT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String tokenCert;

    @ApiModelProperty(value = GatewayModelDescription.KNOX_GATEWAY_TYPE, allowableValues = "CENTRAL,INDIVIDUAL")
    private GatewayType gatewayType;

    @ApiModelProperty(value = GatewayModelDescription.KNOX_SSO_TYPE, allowableValues = "SSO_PROVIDER,NONE")
    private SSOType ssoType;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<GatewayTopologyV4Request> getTopologies() {
        return topologies;
    }

    public void setTopologies(List<GatewayTopologyV4Request> topologies) {
        this.topologies = topologies;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public void setSsoProvider(String ssoProvider) {
        this.ssoProvider = ssoProvider;
    }

    public String getTokenCert() {
        return tokenCert;
    }

    public void setTokenCert(String tokenCert) {
        this.tokenCert = tokenCert;
    }

    public GatewayType getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(GatewayType gatewayType) {
        this.gatewayType = gatewayType;
    }

    public SSOType getSsoType() {
        return ssoType;
    }

    public void setSsoType(SSOType ssoType) {
        this.ssoType = ssoType;
    }
}