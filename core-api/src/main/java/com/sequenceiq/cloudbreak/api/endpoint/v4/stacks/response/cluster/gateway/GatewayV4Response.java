package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.GatewayModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GatewayV4Response implements JsonEntity {

    @JsonIgnore
    @Schema(description = GatewayModelDescription.KNOX_PATH)
    private String path;

    @Schema(description = GatewayModelDescription.GATEWAY_TOPOLOGIES, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<GatewayTopologyV4Response> topologies = new ArrayList<>();

    @JsonIgnore
    @Schema(description = GatewayModelDescription.KNOX_SSO_PROVIDER)
    private String ssoProvider;

    @Schema(description = GatewayModelDescription.KNOX_SSO_CERT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String tokenCert;

    @Schema(description = GatewayModelDescription.KNOX_GATEWAY_TYPE)
    private GatewayType gatewayType;

    @Schema(description = GatewayModelDescription.KNOX_SSO_TYPE)
    private SSOType ssoType;

    private String gatewaySigningPublicKey;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<GatewayTopologyV4Response> getTopologies() {
        return topologies;
    }

    public void setTopologies(List<GatewayTopologyV4Response> topologies) {
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

    public String getGatewaySigningPublicKey() {
        return gatewaySigningPublicKey;
    }

    public void setGatewaySigningPublicKey(String gatewaySigningPublicKey) {
        this.gatewaySigningPublicKey = gatewaySigningPublicKey;
    }
}
